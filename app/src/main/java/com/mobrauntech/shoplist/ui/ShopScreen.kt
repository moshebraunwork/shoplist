package com.mobrauntech.shoplist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mobrauntech.shoplist.ui.components.AddItemSheet
import com.mobrauntech.shoplist.ui.components.BottomCountBar
import com.mobrauntech.shoplist.ui.components.CompleteItemsSection
import com.mobrauntech.shoplist.ui.components.DetailsSheet
import com.mobrauntech.shoplist.ui.components.SectionHeader
import com.mobrauntech.shoplist.ui.components.SwipeableItemCard
import com.mobrauntech.shoplist.ui.components.TopBar
import com.mobrauntech.shoplist.ui.theme.Accent
import com.mobrauntech.shoplist.ui.theme.Bg

@Composable
fun ShopScreen(vm: ShopViewModel) {
    val mode by vm.mode.collectAsState()
    val groups by vm.groups.collectAsState()
    val finished by vm.finished.collectAsState()
    val remaining by vm.remainingCount.collectAsState()
    val pending by vm.dirtyCount.collectAsState()
    val sheet by vm.sheet.collectAsState()
    val details by vm.details.collectAsState()

    var completeExpanded by remember { mutableStateOf(false) }

    // Sync runs only while the screen is in the foreground.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.startSync() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.stopSync() }

    // Background fills the whole screen; content is inset to the safe area so it
    // never sits under the status bar or gesture bar (auto-measured per device).
    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {

            TopBar(mode = mode, pendingSync = pending, onSelect = vm::setMode)

            // ---- Grouped list ----
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
            ) {
                groups.forEach { group ->
                    item(key = "section_${group.name}") { SectionHeader(group.name) }
                    items(group.items.size, key = { idx -> group.items[idx].id }) { idx ->
                        val itemEntity = group.items[idx]
                        if (mode == Mode.SHOP) {
                            SwipeableItemCard(
                                item = itemEntity,
                                rightIsEdit = false,          // right = view details
                                leftIsComplete = true,         // left = complete
                                onSwipeRight = { vm.openDetails(itemEntity) },
                                onSwipeLeft = { vm.complete(itemEntity) }
                            )
                        } else {
                            SwipeableItemCard(
                                item = itemEntity,
                                rightIsEdit = true,            // right = edit
                                leftIsComplete = false,        // left = delete
                                onSwipeRight = { vm.openEdit(itemEntity) },
                                onSwipeLeft = { vm.delete(itemEntity) }
                            )
                        }
                    }
                }
            }

            // ---- Bottom area (both modes) ----
            CompleteItemsSection(
                finished = finished,
                expanded = completeExpanded,
                onToggle = { completeExpanded = !completeExpanded },
                onRestore = vm::restore
            )
            BottomCountBar(
                if (mode == Mode.SHOP) "$remaining Items remaining"
                else "$remaining Items in the list"
            )
        }

        // ---- Add FAB (Add mode only) ----
        if (mode == Mode.ADD) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(end = 24.dp, bottom = 92.dp)
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Accent)
                    .clickable { vm.openAdd() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add item",
                    tint = Bg,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }

    // ---- Add / edit sheet ----
    if (sheet.visible) {
        AddItemSheet(
            state = sheet,
            onNameChange = vm::onNameChange,
            onDescriptionChange = vm::onDescriptionChange,
            onCountChange = vm::onCountChange,
            onChooseImage = vm::chooseImage,
            onUpload = vm::uploadImage,
            onApplyReuse = vm::applyReuse,
            onEditDuplicate = vm::editDuplicate,
            onSubmit = vm::submit,
            onDismiss = vm::closeSheet
        )
    }

    // ---- Read-only details (Shop-mode swipe right) ----
    details?.let { item ->
        DetailsSheet(item = item, onDismiss = vm::closeDetails)
    }
}
