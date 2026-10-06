package com.nehamahesh.pocketalpha

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import org.json.JSONArray
import org.json.JSONObject

data class WidgetQuote(
    val symbol: String,
    val price: Double,
    val changePercent: Double
)

class WidgetSnapshotStore(context: Context) {
    private val prefs = context.getSharedPreferences("pocketalpha_widget", Context.MODE_PRIVATE)

    fun save(quotes: List<Quote>) {
        val array = JSONArray()
        quotes.take(5).forEach { quote ->
            array.put(
                JSONObject()
                    .put("symbol", quote.symbol)
                    .put("price", quote.price)
                    .put("changePercent", quote.changePercent)
            )
        }
        prefs.edit().putString("watchlist", array.toString()).apply()
    }

    fun clear() {
        prefs.edit().remove("watchlist").apply()
    }

    fun load(): List<WidgetQuote> {
        val raw = prefs.getString("watchlist", "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                WidgetQuote(
                    symbol = item.getString("symbol"),
                    price = item.getDouble("price"),
                    changePercent = item.getDouble("changePercent")
                )
            }
        }.getOrDefault(emptyList())
    }
}

class PocketAlphaWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val quotes = WidgetSnapshotStore(context).load()
        provideContent {
            WatchlistWidgetContent(quotes)
        }
    }

    suspend fun refresh(context: Context) = updateAll(context)
}

@Composable
private fun WatchlistWidgetContent(quotes: List<WidgetQuote>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Text(
            text = "PocketAlpha Watchlist",
            style = TextStyle(
                color = ColorProvider(Color(0xFF111827)),
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.height(10.dp))

        if (quotes.isEmpty()) {
            Text("Open PocketAlpha and add stocks to your watchlist.")
        } else {
            quotes.forEach { quote ->
                Row(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(
                        quote.symbol,
                        modifier = GlanceModifier.width(64.dp),
                        style = TextStyle(fontWeight = FontWeight.Bold)
                    )
                    Text("$${"%.2f".format(quote.price)}")
                    Spacer(GlanceModifier.width(10.dp))
                    Text("${if (quote.changePercent >= 0) "+" else ""}${"%.2f".format(quote.changePercent)}%")
                }
            }
        }
    }
}

class PocketAlphaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PocketAlphaWidget()
}
