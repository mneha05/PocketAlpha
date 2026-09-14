package com.nehamahesh.pocketalpha

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private val moneyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

fun money(value: Double): String = moneyFormatter.format(value)
fun quantity(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.4f".format(value).trimEnd('0')
fun percent(value: Double): String = "${if (value >= 0) "+" else ""}${"%.2f".format(value)}%"

@Composable
fun AlphaLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(42.dp)
            .background(AlphaColors.Green, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(23.dp)) {
            val path = Path().apply {
                moveTo(size.width * .12f, size.height * .76f)
                lineTo(size.width * .42f, size.height * .22f)
                lineTo(size.width * .6f, size.height * .53f)
                lineTo(size.width * .86f, size.height * .12f)
            }
            drawPath(path, Color(0xFF111800), style = Stroke(width = 4.dp.toPx()))
            drawCircle(Color(0xFF111800), radius = 2.4.dp.toPx(), center = Offset(size.width * .86f, size.height * .12f))
        }
    }
}

@Composable
fun AlphaButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, loading: Boolean = false, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AlphaColors.Green,
            contentColor = Color(0xFF101700),
            disabledContainerColor = AlphaColors.Green.copy(alpha = .45f)
        )
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF101700))
        else Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SectionHeading(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (action != null && onAction != null) Text(action, color = AlphaColors.Green, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickable(onClick = onAction).padding(8.dp))
    }
}

@Composable
fun QuoteRow(quote: Quote, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(45.dp).background(symbolColor(quote.symbol), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(quote.symbol.take(1), color = AlphaColors.Background, fontWeight = FontWeight.Black)
        }
        Column(Modifier.padding(start = 13.dp).weight(1f)) {
            Text(quote.symbol, fontWeight = FontWeight.SemiBold)
            Text(quote.name, color = AlphaColors.Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        MiniSparkline(positive = quote.changePercent >= 0, seed = quote.symbol.sumOf { it.code }, modifier = Modifier.size(66.dp, 31.dp))
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 14.dp)) {
            Text(money(quote.price), fontWeight = FontWeight.Medium)
            Text(percent(quote.changePercent), color = if (quote.changePercent >= 0) AlphaColors.Green else AlphaColors.Coral, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun MiniSparkline(positive: Boolean, seed: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val path = Path()
        for (i in 0..14) {
            val x = size.width * i / 14f
            val wave = kotlin.math.sin(seed * .02 + i * .83).toFloat() * size.height * .14f
            val trend = (if (positive) -1 else 1) * size.height * .28f * (i / 14f - .5f)
            val y = size.height / 2 + wave + trend
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, if (positive) AlphaColors.Green else AlphaColors.Coral, style = Stroke(2.dp.toPx()))
    }
}

@Composable
fun PriceChart(points: List<HistoryPoint>, positive: Boolean, modifier: Modifier = Modifier) {
    val reveal by animateFloatAsState(if (points.isEmpty()) 0f else 1f, label = "chart")
    val lineColor by animateColorAsState(if (positive) AlphaColors.Green else AlphaColors.Coral, label = "chartColor")
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val low = points.minOf { it.value }
        val high = points.maxOf { it.value }
        val span = max(high - low, .01)
        val inset = 5.dp.toPx()
        val chartHeight = size.height - inset * 2
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = index.toFloat() / (points.size - 1) * size.width * reveal
            val y = inset + ((high - point.value) / span * chartHeight).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawLine(AlphaColors.Line.copy(alpha = .7f), Offset(0f, size.height * .25f), Offset(size.width, size.height * .25f), strokeWidth = 1f)
        drawLine(AlphaColors.Line.copy(alpha = .7f), Offset(0f, size.height * .75f), Offset(size.width, size.height * .75f), strokeWidth = 1f)
        drawPath(path, lineColor, style = Stroke(width = 2.5.dp.toPx()))
        val fillPath = Path().apply {
            addPath(path)
            lineTo(size.width * reveal, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fillPath, Brush.verticalGradient(listOf(lineColor.copy(alpha = .22f), Color.Transparent)))
    }
}

@Composable
fun StatPill(label: String, value: String, positive: Boolean? = null, modifier: Modifier = Modifier) {
    Column(modifier.border(1.dp, AlphaColors.Line, RoundedCornerShape(18.dp)).padding(15.dp)) {
        Text(label, color = AlphaColors.Muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Text(value, fontWeight = FontWeight.SemiBold, color = when (positive) { true -> AlphaColors.Green; false -> AlphaColors.Coral; null -> AlphaColors.Text })
    }
}

@Composable
fun EmptyMessage(title: String, detail: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.SearchOff, null, tint = AlphaColors.Muted, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(detail, color = AlphaColors.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
fun DisclosureCard() {
    Row(Modifier.fillMaxWidth().background(AlphaColors.Surface, RoundedCornerShape(18.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.ArrowOutward, null, tint = AlphaColors.Green, modifier = Modifier.size(20.dp))
        Text("Simulated prices · Educational use only", color = AlphaColors.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
    }
}

private fun symbolColor(symbol: String): Color {
    val palette = listOf(Color(0xFFB7F64A), Color(0xFF7BDFF2), Color(0xFFFFD166), Color(0xFFCDB4DB), Color(0xFFFF8FAB))
    return palette[kotlin.math.abs(symbol.hashCode()) % palette.size]
}
