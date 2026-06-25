package com.mobrauntech.shoplist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mobrauntech.shoplist.data.local.ItemEntity
import com.mobrauntech.shoplist.ui.theme.DeleteRed
import com.mobrauntech.shoplist.ui.theme.TextSecondary

private val CompleteGreen = Color(0xFF2E7D43)

/**
 * Right swipe (StartToEnd) triggers [onSwipeRight] and snaps back — non-destructive
 * (view details in Shop, edit in Add).
 * Left swipe (EndToStart) triggers [onSwipeLeft] and dismisses the row — complete in
 * Shop mode, delete in Add mode.
 */
@Composable
fun SwipeableItemCard(
    item: ItemEntity,
    rightIsEdit: Boolean,        // true in Add mode (edit), false in Shop mode (details)
    leftIsComplete: Boolean,     // true in Shop mode (complete), false in Add mode (delete)
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onSwipeRight(); false } // snap back
                SwipeToDismissBoxValue.EndToStart -> { onSwipeLeft(); true }    // dismiss
                else -> false
            }
        },
        positionalThreshold = { total -> total * 0.6f }
    )

    SwipeToDismissBox(
        state = state,
        modifier = modifier.fillMaxWidth(),
        backgroundContent = {
            val toEnd = state.targetValue == SwipeToDismissBoxValue.StartToEnd
            val (bg, align, icon) = if (toEnd) {
                Triple(
                    Color(0xFF26262A),
                    Alignment.CenterStart,
                    if (rightIsEdit) Icons.Filled.Edit else Icons.Filled.Info
                )
            } else {
                Triple(
                    if (leftIsComplete) CompleteGreen else DeleteRed,
                    Alignment.CenterEnd,
                    if (leftIsComplete) Icons.Filled.Check else Icons.Filled.Delete
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg)
                    .padding(horizontal = 22.dp),
                contentAlignment = align
            ) {
                Icon(icon, contentDescription = null, tint = if (toEnd) TextSecondary else Color.White)
            }
        }
    ) {
        ItemCard(item = item, modifier = Modifier.fillMaxWidth())
    }
}
