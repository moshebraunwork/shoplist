package com.mobrauntech.shoplist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.mobrauntech.shoplist.ui.theme.Card
import com.mobrauntech.shoplist.ui.theme.TextPrimary
import com.mobrauntech.shoplist.ui.theme.TextSecondary

/**
 * Vertical drag changes the count: drag up to increase, down to decrease.
 * The ticks on the right echo the "ruler" in the reference design.
 */
@Composable
fun CountScroller(
    count: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pixels of drag per ±1 step.
    val stepPx = 22f
    var accumulated by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .height(64.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { accumulated = 0f }
                ) { change, dragAmount ->
                    change.consume()
                    accumulated += -dragAmount // up (negative dy) -> increase
                    while (accumulated >= stepPx) { onChange(count + 1); accumulated -= stepPx }
                    while (accumulated <= -stepPx) { onChange((count - 1).coerceAtLeast(1)); accumulated += stepPx }
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }
        // Ruler ticks.
        Canvas(
            modifier = Modifier
                .width(16.dp)
                .fillMaxHeight()
                .padding(vertical = 10.dp, end = 4.dp)
        ) {
            val ticks = 7
            val gap = size.height / (ticks - 1)
            for (i in 0 until ticks) {
                val y = i * gap
                val len = if (i % 2 == 0) size.width else size.width * 0.5f
                drawLine(
                    color = Color(0xFF5A5A5F),
                    start = Offset(size.width - len, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2f
                )
            }
        }
    }
}
