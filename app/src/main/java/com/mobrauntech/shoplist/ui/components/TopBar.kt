package com.mobrauntech.shoplist.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobrauntech.shoplist.ui.Mode
import com.mobrauntech.shoplist.ui.theme.Accent
import com.mobrauntech.shoplist.ui.theme.TextPrimary
import com.mobrauntech.shoplist.ui.theme.TextSecondary
import com.mobrauntech.shoplist.ui.theme.TopBar

private val TabWidth = 64.dp
private val IndicatorWidth = 30.dp

@Composable
fun TopBar(
    mode: Mode,
    pendingSync: Int,
    onSelect: (Mode) -> Unit
) {
    Surface(
        color = TopBar,
        shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Shopping list", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                if (pendingSync > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Accent.copy(alpha = 0.7f))
                    )
                }
            }

            // Tabs with a sliding underline.
            Column(horizontalAlignment = Alignment.Start) {
                Row {
                    Tab("Add", active = mode == Mode.ADD) { onSelect(Mode.ADD) }
                    Tab("Shop", active = mode == Mode.SHOP) { onSelect(Mode.SHOP) }
                }
                // Center the indicator under whichever tab is active, and animate the slide.
                val centerInCell = (TabWidth - IndicatorWidth) / 2
                val targetX = if (mode == Mode.ADD) centerInCell else TabWidth + centerInCell
                val indicatorX by animateDpAsState(
                    targetValue = targetX,
                    animationSpec = tween(durationMillis = 220),
                    label = "tabIndicator"
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .offset(x = indicatorX)
                        .width(IndicatorWidth)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(Accent)
                )
            }
        }
    }
}

@Composable
private fun Tab(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (active) TextPrimary else TextSecondary,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .width(TabWidth)
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    )
}
