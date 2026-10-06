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
    val bluetoothDevices: List<NearbyBluetoothDevice> = emptyList(),
    val bluetoothStatus: String = "Bluetooth idle",
    val bluetoothScanning: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

/**
 * ViewModel in PocketAlpha's MVVM stack.
 *
 * Compose observes immutable StateFlow state while all data/network work is delegated
 * to PocketAlphaRepository. viewModelScope gives every request lifecycle-aware
 * cancellation when the ViewModel is cleared.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val sessions = SessionStore(application)
    private val repository = PocketAlphaRepository(ApiClient(sessions))

    private val _state = MutableStateFlow(PocketAlphaState())
    val state: StateFlow<PocketAlphaState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private val bluetooth = BluetoothMarketLink(
        context = application,
        onDevicesChanged = { devices ->
            _state.update { it.copy(bluetoothDevices = devices) }
        },
        onStatusChanged = { status ->
            _state.update {
                it.copy(
                    bluetoothStatus = status,
                    bluetoothScanning = status.startsWith("Scanning")
                )
            }
        }
    )

    init {
        bootstrap()
    }

    private fun bootstrap() = viewModelScope.launch {
        _state.update { it.copy(booting = true, error = null) }

        val market = runCatching { repository.dashboard(authenticated = false).market }.getOrNull()
        if (sessions.token == null) {
            _state.update { it.copy(booting = false, market = market) }
            return@launch
        }

        runCatching { repository.sessionSnapshot() }
            .onSuccess { snapshot ->
                _state.update {
                    it.copy(
                        booting = false,
                        user = snapshot.user,
                        market = market,
                        watchlist = snapshot.watchlist,
                        portfolio = snapshot.portfolio
                    )
                }
                activateBackgroundFeatures(snapshot.watchlist)
            }
            .onFailure {
                sessions.clear()
                _state.update {
                    it.copy(
                        booting = false,
                        market = market,
                        user = null,
                        error = "Your session expired. Please sign in again."
                    )
                }
            }
    }

    fun login(email: String, password: String) =
        auth { repository.login(email, password) }

    fun register(name: String, email: String, password: String) =
        auth { repository.register(name, email, password) }

    private fun auth(block: suspend () -> AuthResult) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching { block() }
            .onSuccess { result ->
                runCatching { repository.accountData() }
                    .onSuccess { (watchlist, portfolio) ->
                        _state.update {
                            it.copy(
                                loading = false,
                                user = result.user,
                                watchlist = watchlist,
                                portfolio = portfolio
                            )
                        }
                        activateBackgroundFeatures(watchlist)
                    }
                    .onFailure(::showError)
            }
            .onFailure(::showError)
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }

        runCatching { repository.dashboard(authenticated = _state.value.user != null) }
            .onSuccess { dashboard ->
                _state.update {
                    it.copy(
                        loading = false,
                        market = dashboard.market,
                        watchlist = dashboard.watchlist,
                        portfolio = dashboard.portfolio
                    )
                }
                syncWidget(dashboard.watchlist)
            }
            .onFailure(::showError)
    }

    fun search(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(250)
            runCatching { repository.quotes(query) }
                .onSuccess { results ->
                    _state.update {
                        it.copy(searchResults = results, error = null)
                    }
                }
                .onFailure(::showError)
        }
    }

    fun selectStock(
        symbol: String,
        range: String = _state.value.selectedRange
    ) = viewModelScope.launch {
        _state.update {
            it.copy(
                loading = true,
                error = null,
                selectedRange = range
            )
        }

        runCatching { repository.history(symbol, range) }
            .onSuccess { history ->
                _state.update {
                    it.copy(loading = false, selected = history)
                }
            }
            .onFailure(::showError)
    }

    fun closeStock() =
        _state.update { it.copy(selected = null) }

    fun toggleWatchlist(symbol: String) = viewModelScope.launch {
        val saved = _state.value.watchlist.any { it.symbol == symbol }

        runCatching {
            repository.toggleWatchlist(symbol, saved)
        }.onSuccess { list ->
            _state.update {
                it.copy(
                    watchlist = list,
                    notice = if (saved) "$symbol removed" else "$symbol added"
                )
            }
            syncWidget(list)
        }.onFailure(::showError)
    }

    fun placeOrder(
        symbol: String,
        side: String,
        quantity: Double,
        onComplete: () -> Unit
    ) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }

        runCatching {
            repository.placeOrder(symbol, side, quantity)
        }.onSuccess { portfolio ->
            _state.update {
                it.copy(
                    loading = false,
                    portfolio = portfolio,
                    notice = "$side order filled"
                )
            }
            onComplete()
        }.onFailure(::showError)
    }

    fun clearMessage() =
        _state.update { it.copy(error = null, notice = null) }

    fun scanBluetooth() {
        bluetooth.startScan()
    }

    fun connectBluetooth(address: String) {
        bluetooth.connect(address)
    }

    private fun activateBackgroundFeatures(watchlist: List<Quote>) {
        PriceAlertScheduler.schedule(getApplication())
        syncWidget(watchlist)
    }

    private fun syncWidget(watchlist: List<Quote>) {
        WidgetSnapshotStore(getApplication()).save(watchlist)
        viewModelScope.launch {
            PocketAlphaWidget().refresh(getApplication())
        }
    }

    fun logout() {
        sessions.clear()
        PriceAlertScheduler.cancel(getApplication())
        bluetooth.close()
        WidgetSnapshotStore(getApplication()).clear()
        viewModelScope.launch {
            PocketAlphaWidget().refresh(getApplication())
        }
        _state.update {
            PocketAlphaState(
                booting = false,
                market = it.market
            )
        }
    }

    override fun onCleared() {
        bluetooth.close()
        super.onCleared()
    }

    private fun showError(throwable: Throwable) {
        val message = when (throwable) {
            is ApiException -> throwable.message
            else -> "Could not reach PocketAlpha. Check that the backend is running."
        }

        _state.update {
            it.copy(
                loading = false,
                error = message
            )
        }
    }
}
