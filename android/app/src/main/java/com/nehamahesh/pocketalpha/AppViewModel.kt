package com.nehamahesh.pocketalpha

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PocketAlphaState(
    val booting: Boolean = true,
    val loading: Boolean = false,
    val user: User? = null,
    val market: MarketOverview? = null,
    val watchlist: List<Quote> = emptyList(),
    val portfolio: Portfolio = Portfolio(),
    val searchResults: List<Quote> = emptyList(),
    val selected: StockHistory? = null,
    val selectedRange: String = "1D",
    val error: String? = null,
    val notice: String? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val sessions = SessionStore(application)
    private val api = ApiClient(sessions)
    private val _state = MutableStateFlow(PocketAlphaState())
    val state: StateFlow<PocketAlphaState> = _state.asStateFlow()
    private var searchJob: Job? = null

    init { bootstrap() }

    private fun bootstrap() = viewModelScope.launch {
        _state.update { it.copy(booting = true, error = null) }
        val market = runCatching { api.market() }.getOrNull()
        if (sessions.token == null) {
            _state.update { it.copy(booting = false, market = market) }
            return@launch
        }
        runCatching {
            val user = api.me()
            val watchlist = api.watchlist()
            val portfolio = api.portfolio()
            Triple(user, watchlist, portfolio)
        }.onSuccess { (user, watchlist, portfolio) ->
            _state.update { it.copy(booting = false, user = user, market = market, watchlist = watchlist, portfolio = portfolio) }
        }.onFailure {
            sessions.clear()
            _state.update { it.copy(booting = false, market = market, user = null, error = "Your session expired. Please sign in again.") }
        }
    }

    fun login(email: String, password: String) = auth { api.login(email, password) }
    fun register(name: String, email: String, password: String) = auth { api.register(name, email, password) }

    private fun auth(block: suspend () -> AuthResult) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching { block() }
            .onSuccess { result ->
                val watchlist = api.watchlist()
                val portfolio = api.portfolio()
                _state.update { it.copy(loading = false, user = result.user, watchlist = watchlist, portfolio = portfolio) }
            }
            .onFailure(::showError)
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            val market = api.market()
            val watchlist = if (_state.value.user != null) api.watchlist() else emptyList()
            val portfolio = if (_state.value.user != null) api.portfolio() else Portfolio()
            Triple(market, watchlist, portfolio)
        }.onSuccess { (market, watchlist, portfolio) ->
            _state.update { it.copy(loading = false, market = market, watchlist = watchlist, portfolio = portfolio) }
        }.onFailure(::showError)
    }

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(250)
            runCatching { api.quotes(query) }
                .onSuccess { results -> _state.update { it.copy(searchResults = results, error = null) } }
                .onFailure(::showError)
        }
    }

    fun selectStock(symbol: String, range: String = _state.value.selectedRange) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null, selectedRange = range) }
        runCatching { api.history(symbol, range) }
            .onSuccess { history -> _state.update { it.copy(loading = false, selected = history) } }
            .onFailure(::showError)
    }

    fun closeStock() = _state.update { it.copy(selected = null) }

    fun toggleWatchlist(symbol: String) = viewModelScope.launch {
        val saved = _state.value.watchlist.any { it.symbol == symbol }
        runCatching { if (saved) api.remove(symbol) else api.save(symbol) }
            .onSuccess {
                val list = api.watchlist()
                _state.update { it.copy(watchlist = list, notice = if (saved) "$symbol removed" else "$symbol added") }
            }.onFailure(::showError)
    }

    fun placeOrder(symbol: String, side: String, quantity: Double, onComplete: () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching { api.placeOrder(symbol, side, quantity) }
            .onSuccess { portfolio ->
                _state.update { it.copy(loading = false, portfolio = portfolio, notice = "$side order filled") }
                onComplete()
            }.onFailure(::showError)
    }

    fun clearMessage() = _state.update { it.copy(error = null, notice = null) }

    fun logout() {
        sessions.clear()
        _state.update { PocketAlphaState(booting = false, market = it.market) }
    }

    private fun showError(throwable: Throwable) {
        val message = when (throwable) {
            is ApiException -> throwable.message
            else -> "Could not reach PocketAlpha. Check that the backend is running."
        }
        _state.update { it.copy(loading = false, error = message) }
    }
}
