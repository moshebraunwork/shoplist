package com.mobrauntech.shoplist.data.repo

import android.content.Context
import android.net.Uri
import com.mobrauntech.shoplist.data.local.AppDatabase
import com.mobrauntech.shoplist.data.local.ItemEntity
import com.mobrauntech.shoplist.data.local.STATUS_ACTIVE
import com.mobrauntech.shoplist.data.local.STATUS_COMPLETE
import com.mobrauntech.shoplist.data.local.STATUS_DELETED
import com.mobrauntech.shoplist.data.local.SectionEntity
import com.mobrauntech.shoplist.data.local.SyncPrefs
import com.mobrauntech.shoplist.data.remote.Api
import com.mobrauntech.shoplist.data.remote.SectionRequest
import com.mobrauntech.shoplist.data.remote.SuggestResponse
import com.mobrauntech.shoplist.data.remote.SyncRequest
import com.mobrauntech.shoplist.data.remote.toDto
import com.mobrauntech.shoplist.data.remote.toEntity
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class ShopRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)
    private val itemDao = db.itemDao()
    private val sectionDao = db.sectionDao()
    private val api = Api.service
    private val prefs = SyncPrefs(appContext)

    val activeItems: Flow<List<ItemEntity>> = itemDao.activeItems()
    val finishedItems: Flow<List<ItemEntity>> = itemDao.finishedItems()
    val sections: Flow<List<SectionEntity>> = sectionDao.sections()
    val dirtyCount: Flow<Int> = itemDao.dirtyCount()

    private fun now() = System.currentTimeMillis()

    // ---------- Add / update ----------

    /** Adds a brand-new item. Resolves its section via AI (with an "Other" fallback). */
    suspend fun addItem(name: String, description: String, count: Int, imageUrl: String?) {
        val ts = now()
        val section = resolveSection(name, description)
        ensureSectionLocal(section, ts)
        val item = ItemEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            description = description.trim(),
            count = count.coerceAtLeast(1),
            section = section,
            imageUrl = imageUrl,
            status = STATUS_ACTIVE,
            createdAt = ts,
            nameUpdatedAt = ts, descUpdatedAt = ts, countUpdatedAt = ts,
            sectionUpdatedAt = ts, imageUpdatedAt = ts, statusUpdatedAt = ts,
            updatedAt = ts,
            dirty = true
        )
        itemDao.upsert(item)
    }

    /** Updates an existing item's user-editable fields. Stamps only what the user touched. */
    suspend fun updateItem(id: String, name: String, description: String, count: Int, imageUrl: String?) {
        val existing = itemDao.getById(id) ?: return
        val ts = now()
        val nameChanged = name.trim() != existing.name
        val descChanged = description.trim() != existing.description
        val countChanged = count != existing.count
        val imageChanged = imageUrl != existing.imageUrl
        val updated = existing.copy(
            name = name.trim(),
            description = description.trim(),
            count = count.coerceAtLeast(1),
            imageUrl = imageUrl,
            nameUpdatedAt = if (nameChanged) ts else existing.nameUpdatedAt,
            descUpdatedAt = if (descChanged) ts else existing.descUpdatedAt,
            countUpdatedAt = if (countChanged) ts else existing.countUpdatedAt,
            imageUpdatedAt = if (imageChanged) ts else existing.imageUpdatedAt,
            updatedAt = ts,
            dirty = true
        )
        itemDao.upsert(updated)
    }

    /** Shop-mode complete: move to the completed list. */
    suspend fun completeItem(id: String) = setStatus(id, STATUS_COMPLETE)

    /** Left-swipe delete: move to completed list with the deleted flag. */
    suspend fun deleteItem(id: String) = setStatus(id, STATUS_DELETED)

    /** Bring a completed/deleted item back onto the active list. */
    suspend fun restoreItem(id: String) {
        val existing = itemDao.getById(id) ?: return
        val ts = now()
        itemDao.upsert(
            existing.copy(
                status = STATUS_ACTIVE,
                completedAt = null,
                statusUpdatedAt = ts,
                updatedAt = ts,
                dirty = true
            )
        )
    }

    private suspend fun setStatus(id: String, status: String) {
        val existing = itemDao.getById(id) ?: return
        val ts = now()
        itemDao.upsert(
            existing.copy(
                status = status,
                completedAt = ts,
                statusUpdatedAt = ts,
                updatedAt = ts,
                dirty = true
            )
        )
    }

    private suspend fun ensureSectionLocal(name: String, ts: Long) {
        if (sectionDao.getByName(name) == null) {
            val pos = sectionDao.maxPosition() + 1
            sectionDao.upsert(SectionEntity(name = name, position = pos, updatedAt = ts, dirty = true))
        }
    }

    private suspend fun resolveSection(name: String, description: String): String =
        runCatching { api.aiSection(SectionRequest(name, description)).section }
            .getOrNull()?.ifBlank { "Other" } ?: "Other"

    // ---------- As-you-type ----------

    suspend fun suggest(query: String): SuggestResponse =
        runCatching { api.suggest(query) }.getOrElse { SuggestResponse() }

    suspend fun searchImages(query: String): List<String> =
        runCatching { api.images(query).images }.getOrElse { emptyList() }

    suspend fun uploadImage(uri: Uri): String? = runCatching {
        val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return null
        val body = bytes.toRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("image", "upload.jpg", body)
        api.upload(part).url
    }.getOrNull()

    // ---------- Sync ----------

    /**
     * One sync cycle: push every dirty row, then apply whatever the server returns.
     * Returns true if the network call succeeded.
     */
    suspend fun syncOnce(): Boolean = runCatching {
        val dirtyItems = itemDao.getDirty()
        val dirtySections = sectionDao.getDirty()

        val resp = api.sync(
            SyncRequest(
                since = prefs.since,
                items = dirtyItems.map { it.toDto() },
                sections = dirtySections.map { it.toDto() }
            )
        )

        // Apply server's authoritative rows (clean).
        if (resp.sections.isNotEmpty()) {
            sectionDao.upsertAll(resp.sections.map { it.toEntity(dirty = false) })
        }
        if (resp.items.isNotEmpty()) {
            // Don't overwrite a row the user edited again after we sent it.
            val stillDirty = itemDao.getDirty().associateBy { it.id }
            val toApply = resp.items.mapNotNull { dto ->
                val localDirty = stillDirty[dto.id]
                if (localDirty != null && localDirty.updatedAt > dto.updatedAt) null
                else dto.toEntity(dirty = false)
            }
            if (toApply.isNotEmpty()) itemDao.upsertAll(toApply)
        }

        // Clear dirty flags for the rows we successfully pushed (unless re-edited mid-flight).
        val pushedItemIds = dirtyItems
            .filter { sent -> (itemDao.getById(sent.id)?.updatedAt ?: 0) <= sent.updatedAt }
            .map { it.id }
        if (pushedItemIds.isNotEmpty()) itemDao.clearDirty(pushedItemIds)
        if (dirtySections.isNotEmpty()) sectionDao.clearDirty(dirtySections.map { it.name })

        // Advance the watermark past everything we just saw.
        val maxSeen = (resp.items.maxOfOrNull { it.updatedAt } ?: 0L)
            .coerceAtLeast(resp.sections.maxOfOrNull { it.updatedAt } ?: 0L)
        if (maxSeen > prefs.since) prefs.since = maxSeen
        true
    }.getOrElse { false }
}
