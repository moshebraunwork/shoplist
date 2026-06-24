package com.mobrauntech.shoplist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobrauntech.shoplist.ui.Mode
import com.mobrauntech.shoplist.ui.theme.Accent
import com.mobrauntech.shoplist.ui.theme.TextPrimary
import com.mobrauntech.shoplist.ui.theme.TextSecondary
import com.mobrauntech.shoplist.ui.theme.TopBar

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
                .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 18.dp),
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
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Tab("Add", active = mode == Mode.ADD) { onSelect(Mode.ADD) }
                Tab("Shop", active = mode == Mode.SHOP) { onSelect(Mode.SHOP) }
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
        textDecoration = if (active) TextDecoration.Underline else TextDecoration.None,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 2.dp)
    )
}
