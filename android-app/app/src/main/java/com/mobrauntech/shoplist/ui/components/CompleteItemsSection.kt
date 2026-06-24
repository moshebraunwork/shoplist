package com.mobrauntech.shoplist.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobrauntech.shoplist.data.local.ItemEntity
import com.mobrauntech.shoplist.data.local.STATUS_DELETED
import com.mobrauntech.shoplist.ui.theme.Card
import com.mobrauntech.shoplist.ui.theme.TextPrimary
import com.mobrauntech.shoplist.ui.theme.TextSecondary
import com.mobrauntech.shoplist.ui.theme.TopBar

@Composable
fun CompleteItemsSection(
    finished: List<ItemEntity>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRestore: (ItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = expanded) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(finished, key = { it.id }) { item ->
                    CompletedRow(item, onRestore = { onRestore(item) })
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = TextPrimary
            )
            Text("See complete items", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun CompletedRow(item: ItemEntity, onRestore: () -> Unit) {
    val deleted = item.status == STATUS_DELETED
    Surface(color = Card, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(item.count.toString(), color = TextSecondary, fontSize = 18.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    color = if (deleted) TextSecondary else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textDecoration = if (deleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (deleted) Text("deleted", color = Color(0xFFB23A3A), fontSize = 12.sp)
            }
            Icon(
                Icons.Filled.Restore,
                contentDescription = "Restore",
                tint = TextSecondary,
                modifier = Modifier.clickable(onClick = onRestore)
            )
        }
    }
}

@Composable
fun BottomCountBar(text: String) {
    Surface(color = TopBar, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 15.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
