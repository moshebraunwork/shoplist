package com.mobrauntech.shoplist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mobrauntech.shoplist.data.local.ItemEntity
import com.mobrauntech.shoplist.ui.theme.Bg
import com.mobrauntech.shoplist.ui.theme.Card
import com.mobrauntech.shoplist.ui.theme.SectionLabel
import com.mobrauntech.shoplist.ui.theme.TextPrimary
import com.mobrauntech.shoplist.ui.theme.TextSecondary

/** Read-only look at an item — opened by swiping right in Shop mode. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsSheet(item: ItemEntity, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var zoomUrl by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Bg) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!item.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                        .clip(RoundedCornerShape(16.dp)).background(Color.White)
                        .clickable { zoomUrl = item.imageUrl },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Card),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.count.toString(), color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(item.name, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(item.section, color = SectionLabel, fontSize = 14.sp)
                }
            }

            if (item.description.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Notes", color = TextSecondary, fontSize = 12.sp)
                    Text(item.description, color = TextPrimary, fontSize = 16.sp)
                }
            }
        }
    }

    zoomUrl?.let { ZoomableImageDialog(url = it, onDismiss = { zoomUrl = null }) }
}
