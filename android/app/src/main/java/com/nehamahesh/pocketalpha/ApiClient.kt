package com.nehamahesh.pocketalpha

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class SessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("pocketalpha_session", Context.MODE_PRIVATE)

    var token: String?
        get() = preferences.getString("token", null)
        set(value) { preferences.edit().putString("token", value).apply() }

    fun clear() = preferences.edit().clear().apply()
}

class ApiClient(private val sessions: SessionStore) {
    private val baseUrl = BuildConfig.API_BASE_URL

    private suspend fun request(path: String, method: String = "GET", body: JSONObject? = null, authenticated: Boolean = false): JSONObject = withContext(Dispatchers.IO) {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/json")
            if (authenticated) {
                val token = sessions.token ?: throw ApiException("Please sign in again.", 401)
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        body?.let { connection.outputStream.bufferedWriter().use { writer -> writer.write(it.toString()) } }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        val json = if (text.isBlank()) JSONObject() else JSONObject(text)
        if (status !in 200..299) throw ApiException(json.optString("error", "Request failed."), status)
        json
    }

    suspend fun login(email: String, password: String): AuthResult {
        val json = request("auth/login", "POST", JSONObject().put("email", email).put("password", password))
        return parseAuth(json).also { sessions.token = it.token }
    }

    suspend fun register(name: String, email: String, password: String): AuthResult {
        val json = request("auth/register", "POST", JSONObject().put("name", name).put("email", email).put("password", password))
        return parseAuth(json).also { sessions.token = it.token }
    }

    suspend fun me(): User = parseUser(request("me", authenticated = true).getJSONObject("user"))

    suspend fun market(): MarketOverview {
        val json = request("market/overview")
        return MarketOverview(
            indices = json.getJSONArray("indices").objects(::parseIndex),
            movers = json.getJSONArray("movers").objects(::parseQuote),
            asOf = json.getString("asOf"),
            source = json.getString("source")
        )
    }

    suspend fun quotes(query: String = ""): List<Quote> {
        val suffix = if (query.isBlank()) "" else "?query=${URLEncoder.encode(query, Charsets.UTF_8.name())}"
        return request("quotes$suffix").getJSONArray("quotes").objects(::parseQuote)
    }

    suspend fun history(symbol: String, range: String): StockHistory {
        val json = request("stocks/$symbol/history?range=$range")
        return StockHistory(
            quote = parseQuote(json.getJSONObject("quote")),
            range = json.getString("range"),
            points = json.getJSONArray("points").objects { item -> HistoryPoint(item.getString("timestamp"), item.getDouble("value")) }
        )
    }

    suspend fun watchlist(): List<Quote> = request("watchlist", authenticated = true).getJSONArray("quotes").objects(::parseQuote)

    suspend fun save(symbol: String) {
        request("watchlist", "POST", JSONObject().put("symbol", symbol), authenticated = true)
    }

    suspend fun remove(symbol: String) {
        request("watchlist/$symbol", "DELETE", authenticated = true)
    }

    suspend fun portfolio(): Portfolio = parsePortfolio(request("portfolio", authenticated = true))

    suspend fun placeOrder(symbol: String, side: String, quantity: Double): Portfolio {
        val json = request("orders", "POST", JSONObject().put("symbol", symbol).put("side", side).put("quantity", quantity), authenticated = true)
        return parsePortfolio(json.getJSONObject("portfolio"))
    }

    private fun parseAuth(json: JSONObject) = AuthResult(json.getString("token"), parseUser(json.getJSONObject("user")))
    private fun parseUser(json: JSONObject) = User(json.getInt("id"), json.getString("name"), json.getString("email"))
    private fun parseIndex(json: JSONObject) = IndexSnapshot(json.getString("symbol"), json.getDouble("price"), json.getDouble("changePercent"))

    private fun parseQuote(json: JSONObject) = Quote(
        symbol = json.getString("symbol"), name = json.getString("name"), price = json.getDouble("price"),
        previousClose = json.getDouble("previousClose"), change = json.getDouble("change"),
        changePercent = json.getDouble("changePercent"), sector = json.getString("sector"),
        marketCap = json.getLong("marketCap"), isMarketOpen = json.getBoolean("isMarketOpen")
    )

    private fun parsePortfolio(json: JSONObject) = Portfolio(
        cash = json.getDouble("cash"), holdingsValue = json.getDouble("holdingsValue"), totalValue = json.getDouble("totalValue"),
        positions = json.getJSONArray("positions").objects { item ->
            Position(item.getString("symbol"), item.getDouble("quantity"), item.getDouble("averageCost"), item.getDouble("currentPrice"), item.getDouble("marketValue"), item.getDouble("gainLoss"), item.getDouble("gainLossPercent"))
        },
        orders = json.getJSONArray("orders").objects { item ->
            PaperOrder(item.getInt("id"), item.getString("symbol"), item.getString("side"), item.getDouble("quantity"), item.getDouble("price"), item.getDouble("total"), item.getString("createdAt"))
        }
    )
}

private fun <T> JSONArray.objects(transform: (JSONObject) -> T): List<T> = (0 until length()).map { transform(getJSONObject(it)) }
