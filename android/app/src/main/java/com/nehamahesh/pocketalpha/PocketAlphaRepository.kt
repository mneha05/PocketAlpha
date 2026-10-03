package com.nehamahesh.pocketalpha

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class AccountSnapshot(
    val user: User,
    val watchlist: List<Quote>,
    val portfolio: Portfolio
)

data class DashboardSnapshot(
    val market: MarketOverview,
    val watchlist: List<Quote>,
    val portfolio: Portfolio
)

/**
 * Model/data layer for the Android MVVM stack.
 *
 * The ViewModel owns UI state; this repository owns orchestration of remote data.
 * Independent requests are run concurrently with structured coroutines so cancellation
 * from viewModelScope propagates through the entire request tree.
 */
class PocketAlphaRepository(private val api: ApiClient) {
    suspend fun login(email: String, password: String): AuthResult =
        api.login(email, password)

    suspend fun register(name: String, email: String, password: String): AuthResult =
        api.register(name, email, password)

    suspend fun sessionSnapshot(): AccountSnapshot = coroutineScope {
        val user = async { api.me() }
        val watchlist = async { api.watchlist() }
        val portfolio = async { api.portfolio() }

        AccountSnapshot(
            user = user.await(),
            watchlist = watchlist.await(),
            portfolio = portfolio.await()
        )
    }

    suspend fun dashboard(authenticated: Boolean): DashboardSnapshot = coroutineScope {
        val market = async { api.market() }
        val watchlist = if (authenticated) async { api.watchlist() } else null
        val portfolio = if (authenticated) async { api.portfolio() } else null

        DashboardSnapshot(
            market = market.await(),
            watchlist = watchlist?.await().orEmpty(),
            portfolio = portfolio?.await() ?: Portfolio()
        )
    }

    suspend fun accountData(): Pair<List<Quote>, Portfolio> = coroutineScope {
        val watchlist = async { api.watchlist() }
        val portfolio = async { api.portfolio() }
        watchlist.await() to portfolio.await()
    }

    suspend fun quotes(query: String): List<Quote> = api.quotes(query)

    suspend fun history(symbol: String, range: String): StockHistory =
        api.history(symbol, range)

    suspend fun toggleWatchlist(symbol: String, saved: Boolean): List<Quote> {
        if (saved) api.remove(symbol) else api.save(symbol)
        return api.watchlist()
    }

    suspend fun placeOrder(symbol: String, side: String, quantity: Double): Portfolio =
        api.placeOrder(symbol, side, quantity)
}
