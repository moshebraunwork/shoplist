package com.mobrauntech.shoplist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mobrauntech.shoplist.data.local.ItemEntity
import com.mobrauntech.shoplist.data.local.STATUS_DELETED
import com.mobrauntech.shoplist.data.local.SectionEntity
import com.mobrauntech.shoplist.data.remote.ItemDto
import com.mobrauntech.shoplist.data.repo.ShopRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class Mode { SHOP, ADD }

/** A section header followed by its items, ready for the list. */
data class SectionGroup(val name: String, val items: List<ItemEntity>)

/** Everything the add/edit sheet needs while it is open. */
data class SheetState(
    val visible: Boolean = false,
    val editingId: String? = null,        // null => adding, non-null => updating
    val name: String = "",
    val description: String = "",
    val count: Int = 1,
    val imageUrl: String? = null,         // currently chosen image
    val candidates: List<String> = emptyList(), // image suggestions from Google PSE
    val reuse: List<ItemDto> = emptyList(),      // past items to re-use
    val duplicates: List<ItemDto> = emptyList(), // active items that match (warn)
    val uploading: Boolean = false
) {
    val isEditing get() = editingId != null
    val canSubmit get() = name.isNotBlank()
}

class ShopViewModel(private val repo: ShopRepository) : ViewModel() {

    private val _mode = MutableStateFlow(Mode.SHOP)
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    private val _sheet = MutableStateFlow(SheetState())
    val sheet: StateFlow<SheetState> = _sheet.asStateFlow()

    val dirtyCount: StateFlow<Int> =
        repo.dirtyCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    /** Active items grouped by section, in section order. */
    val groups: StateFlow<List<SectionGroup>> =
        combine(repo.activeItems, repo.sections) { items, sections ->
            buildGroups(items, sections)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val finished: StateFlow<List<ItemEntity>> =
        repo.finishedItems.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val remainingCount: StateFlow<Int> =
        repo.activeItems.map { it.size }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private fun buildGroups(items: List<ItemEntity>, sections: List<SectionEntity>): List<SectionGroup> {
        val order = sections.associate { it.name to it.position }
        return items.groupBy { it.section }
            .map { (name, list) -> SectionGroup(name, list) }
            .sortedBy { order[it.name] ?: Int.MAX_VALUE }
    }

    // ---------- Mode ----------
    fun setMode(m: Mode) { _mode.value = m }

    // ---------- Sheet control ----------
    fun openAdd() {
        _sheet.value = SheetState(visible = true)
    }

    fun openEdit(item: ItemEntity) {
        _sheet.value = SheetState(
            visible = true,
            editingId = item.id,
            name = item.name,
            description = item.description,
            count = item.count,
            imageUrl = item.imageUrl,
            candidates = listOfNotNull(item.imageUrl)
        )
    }

    fun closeSheet() {
        lookupJob?.cancel()
        _sheet.value = SheetState()
    }

    fun onNameChange(value: String) {
        _sheet.value = _sheet.value.copy(name = value)
        scheduleLookup(value)
    }

    fun onDescriptionChange(value: String) {
        _sheet.value = _sheet.value.copy(description = value)
    }

    fun onCountChange(value: Int) {
        _sheet.value = _sheet.value.copy(count = value.coerceAtLeast(1))
    }

    fun chooseImage(url: String?) {
        _sheet.value = _sheet.value.copy(imageUrl = url)
    }

    /** Pull values from a past item the user tapped in the "reuse" list. */
    fun applyReuse(dto: ItemDto) {
        _sheet.value = _sheet.value.copy(
            name = dto.name,
            description = dto.description,
            count = dto.count.coerceAtLeast(1),
            imageUrl = dto.imageUrl,
            candidates = listOfNotNull(dto.imageUrl).ifEmpty { _sheet.value.candidates }
        )
    }

    /** Jump to editing the active duplicate the user was warned about. */
    fun editDuplicate(dto: ItemDto) {
        _sheet.value = SheetState(
            visible = true,
            editingId = dto.id,
            name = dto.name,
            description = dto.description,
            count = dto.count,
            imageUrl = dto.imageUrl,
            candidates = listOfNotNull(dto.imageUrl)
        )
    }

    fun uploadImage(uri: android.net.Uri) {
        viewModelScope.launch {
            _sheet.value = _sheet.value.copy(uploading = true)
            val url = repo.uploadImage(uri)
            _sheet.value = _sheet.value.copy(
                uploading = false,
                imageUrl = url ?: _sheet.value.imageUrl,
                candidates = (listOfNotNull(url) + _sheet.value.candidates).distinct().take(3)
            )
        }
    }

    // ---------- Debounced as-you-type lookups ----------
    private var lookupJob: Job? = null
    private fun scheduleLookup(query: String) {
        lookupJob?.cancel()
        val q = query.trim()
        if (q.length < 2) {
            _sheet.value = _sheet.value.copy(candidates = emptyList(), reuse = emptyList(), duplicates = emptyList())
            return
        }
        lookupJob = viewModelScope.launch {
            delay(450) // debounce
            val suggest = repo.suggest(q)
            // Only adopt server image candidates if the user hasn't picked/uploaded one.
            val images = repo.searchImages(q)
            val current = _sheet.value
            if (current.name.trim() == q) {
                _sheet.value = current.copy(
                    reuse = suggest.reuse,
                    duplicates = suggest.duplicates,
                    candidates = if (current.imageUrl == null) images else current.candidates
                )
            }
        }
    }

    // ---------- Submit ----------
    fun submit() {
        val s = _sheet.value
        if (!s.canSubmit) return
        viewModelScope.launch {
            if (s.isEditing) {
                repo.updateItem(s.editingId!!, s.name, s.description, s.count, s.imageUrl)
            } else {
                repo.addItem(s.name, s.description, s.count, s.imageUrl)
            }
            closeSheet()
        }
    }

    // ---------- Swipe / list actions ----------
    fun complete(item: ItemEntity) = viewModelScope.launch { repo.completeItem(item.id) }
    fun delete(item: ItemEntity) = viewModelScope.launch { repo.deleteItem(item.id) }
    fun restore(item: ItemEntity) = viewModelScope.launch { repo.restoreItem(item.id) }

    fun isDeleted(item: ItemEntity) = item.status == STATUS_DELETED

    // ---------- Sync loop (runs only while the app is in foreground) ----------
    private var syncJob: Job? = null

    fun startSync() {
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch {
            // Prime with whatever the server already has.
            repo.syncOnce()
            while (isActive) {
                val ok = repo.syncOnce()
                val dirty = dirtyCount.value > 0
                // Dirty -> retry fast (5s) until clean. Clean -> heartbeat pull every 10s.
                delay(if (dirty || !ok) 5_000 else 10_000)
            }
        }
    }

    fun stopSync() {
        syncJob?.cancel()
        syncJob = null
    }

    class Factory(private val repo: ShopRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ShopViewModel(repo) as T
    }
}
