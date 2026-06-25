package com.mobrauntech.shoplist.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobrauntech.shoplist.data.local.ItemEntity
import com.mobrauntech.shoplist.ui.theme.Card
import com.mobrauntech.shoplist.ui.theme.SectionLabel
import com.mobrauntech.shoplist.ui.theme.TextPrimary
import com.mobrauntech.shoplist.ui.theme.TextSecondary

@Composable
fun ItemCard(item: ItemEntity, modifier: Modifier = Modifier) {
    Surface(
        color = Card,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Count on the left.
            Box(
                modifier = Modifier.width(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.count.toString(),
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = item.name.ifBlank { "Item name here" },
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(name: String) {
    Text(
        text = "—  $name",
        color = SectionLabel,
        fontSize = 15.sp,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 4.dp)
    )
}
