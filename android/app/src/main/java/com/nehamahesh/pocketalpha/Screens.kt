package com.nehamahesh.pocketalpha

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class MainTab(val label: String, val idle: ImageVector, val active: ImageVector) {
    Home("Home", Icons.Outlined.Home, Icons.Rounded.Home),
    Discover("Discover", Icons.Outlined.Search, Icons.Rounded.Search),
    Watchlist("Saved", Icons.Outlined.BookmarkBorder, Icons.Rounded.Bookmark),
    Portfolio("Portfolio", Icons.Outlined.AccountCircle, Icons.Rounded.Person)
}

@Composable
fun LaunchScreen() {
    Box(Modifier.fillMaxSize().background(AlphaColors.Background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AlphaLogo(Modifier.size(58.dp))
            Spacer(Modifier.height(18.dp))
            Text("PocketAlpha", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = AlphaColors.Green, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun AuthScreen(state: PocketAlphaState, onLogin: (String, String) -> Unit, onRegister: (String, String, String) -> Unit, onClearError: () -> Unit) {
    var registering by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().background(AlphaColors.Background).windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AlphaLogo()
            Text("PocketAlpha", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 12.dp))
        }
        Spacer(Modifier.height(58.dp))
        Text(if (registering) "Start with\n$10,000 virtual cash." else "Your market,\nwithout the noise.", style = MaterialTheme.typography.displaySmall)
        Text(
            if (registering) "Learn the mechanics of investing in a zero-risk environment." else "Track companies, understand price movement, and practice investing with confidence.",
            color = AlphaColors.Muted,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 14.dp, bottom = 30.dp)
        )
        AnimatedVisibility(registering) {
            AlphaTextField(value = name, onValueChange = { name = it; onClearError() }, label = "Name")
        }
        AlphaTextField(value = email, onValueChange = { email = it; onClearError() }, label = "Email", keyboardType = KeyboardType.Email)
        AlphaTextField(value = password, onValueChange = { password = it; onClearError() }, label = "Password", isPassword = true)
        AnimatedVisibility(state.error != null) {
            Text(state.error.orEmpty(), color = AlphaColors.Coral, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
        }
        AlphaButton(
            text = if (registering) "Create account" else "Sign in",
            loading = state.loading,
            enabled = email.isNotBlank() && password.length >= 8 && (!registering || name.length >= 2),
            onClick = { if (registering) onRegister(name, email, password) else onLogin(email, password) },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )
        TextButton(
            onClick = { registering = !registering; onClearError() },
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp)
        ) {
            Text(if (registering) "Already have an account? Sign in" else "New here? Create an account", color = AlphaColors.Text)
        }
        if (!registering) {
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = AlphaColors.Line)
                Text("or", color = AlphaColors.Muted, modifier = Modifier.padding(horizontal = 12.dp))
                HorizontalDivider(Modifier.weight(1f), color = AlphaColors.Line)
            }
            TextButton(onClick = { email = "demo@pocketalpha.app"; password = "DemoPass123!"; onLogin(email, password) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Explore the demo account", color = AlphaColors.Green)
            }
        }
        Spacer(Modifier.height(28.dp))
        DisclosureCard()
    }
}

@Composable
private fun AlphaTextField(value: String, onValueChange: (String) -> Unit, label: String, keyboardType: KeyboardType = KeyboardType.Text, isPassword: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AlphaColors.Green,
            unfocusedBorderColor = AlphaColors.Line,
            focusedContainerColor = AlphaColors.Surface,
            unfocusedContainerColor = AlphaColors.Surface,
            focusedLabelColor = AlphaColors.Green
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )
}

@Composable
fun MainShell(state: PocketAlphaState, viewModel: AppViewModel) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Home) }
    Scaffold(
        containerColor = AlphaColors.Background,
        bottomBar = {
            NavigationBar(containerColor = AlphaColors.Surface, tonalElevation = 0.dp, modifier = Modifier.navigationBarsPadding()) {
                MainTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(if (tab == item) item.active else item.idle, item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AlphaColors.Green,
                            selectedTextColor = AlphaColors.Green,
                            indicatorColor = AlphaColors.Green.copy(alpha = .12f),
                            unselectedIconColor = AlphaColors.Muted,
                            unselectedTextColor = AlphaColors.Muted
                        )
                    )
                }
            }
        }
    ) { insets ->
        AnimatedContent(tab, label = "main tabs", modifier = Modifier.padding(insets)) { selectedTab ->
            when (selectedTab) {
                MainTab.Home -> HomeScreen(state, onRefresh = viewModel::refresh, onStock = { viewModel.selectStock(it) }, onDiscover = { tab = MainTab.Discover })
                MainTab.Discover -> DiscoverScreen(state.searchResults, onSearch = viewModel::search, onStock = { viewModel.selectStock(it) })
                MainTab.Watchlist -> WatchlistScreen(state.watchlist, onStock = { viewModel.selectStock(it) }, onDiscover = { tab = MainTab.Discover })
                MainTab.Portfolio -> PortfolioScreen(state, onStock = { viewModel.selectStock(it) }, onLogout = viewModel::logout)
            }
        }
    }
}

@Composable
private fun ScreenHeader(eyebrow: String, title: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(eyebrow.uppercase(), color = AlphaColors.Green, style = MaterialTheme.typography.labelLarge)
            Text(title, style = MaterialTheme.typography.headlineMedium)
        }
        trailing?.invoke()
    }
}

@Composable
fun HomeScreen(state: PocketAlphaState, onRefresh: () -> Unit, onStock: (String) -> Unit, onDiscover: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(AlphaColors.Background)) {
        item {
            ScreenHeader("Good morning", state.user?.name?.substringBefore(" ") ?: "Investor") {
                IconButton(onClick = onRefresh, modifier = Modifier.background(AlphaColors.SurfaceRaised, CircleShape)) {
                    Icon(Icons.Rounded.Refresh, "Refresh")
                }
            }
        }
        item {
            PortfolioHero(state.portfolio)
            Spacer(Modifier.height(28.dp))
        }
        state.market?.indices?.let { indices ->
            item {
                Text("Markets", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    indices.take(3).forEach { snapshot ->
                        Column(Modifier.weight(1f).background(AlphaColors.Surface, RoundedCornerShape(17.dp)).padding(13.dp)) {
                            Text(snapshot.symbol, color = AlphaColors.Muted, style = MaterialTheme.typography.bodyMedium)
                            Text("%,.0f".format(snapshot.price), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 5.dp))
                            Text(percent(snapshot.changePercent), color = if (snapshot.changePercent >= 0) AlphaColors.Green else AlphaColors.Coral, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) { SectionHeading("Today's movement", "Discover", onDiscover) }
        }
        val movers = state.market?.movers.orEmpty()
        if (movers.isEmpty()) item { EmptyMessage("Market data is unavailable", "Start the backend, then tap refresh.") }
        else items(movers, key = { it.symbol }) { quote ->
            QuoteRow(quote, onClick = { onStock(quote.symbol) }, modifier = Modifier.padding(horizontal = 20.dp))
            HorizontalDivider(Modifier.padding(horizontal = 78.dp), color = AlphaColors.Line.copy(alpha = .55f))
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun PortfolioHero(portfolio: Portfolio) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = AlphaColors.Green)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text("TOTAL PORTFOLIO", color = Color(0xFF344300), style = MaterialTheme.typography.labelLarge)
            Text(money(portfolio.totalValue), color = Color(0xFF101700), style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.fillMaxWidth().padding(top = 26.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("Buying power", color = Color(0xFF455900), style = MaterialTheme.typography.bodyMedium); Text(money(portfolio.cash), color = Color(0xFF101700), fontWeight = FontWeight.SemiBold) }
                Column(horizontalAlignment = Alignment.End) { Text("Invested", color = Color(0xFF455900), style = MaterialTheme.typography.bodyMedium); Text(money(portfolio.holdingsValue), color = Color(0xFF101700), fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
fun DiscoverScreen(results: List<Quote>, onSearch: (String) -> Unit, onStock: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().background(AlphaColors.Background)) {
        item { ScreenHeader("Explore", "Find a company") }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; onSearch(it) },
                placeholder = { Text("Search symbol or company") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = ""; onSearch("") }) { Icon(Icons.Rounded.Close, "Clear") } },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AlphaColors.Green, unfocusedBorderColor = AlphaColors.Line, focusedContainerColor = AlphaColors.Surface, unfocusedContainerColor = AlphaColors.Surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        when {
            query.isBlank() -> item { EmptyMessage("Search the market", "Try AAPL, NVIDIA, or Robinhood.") }
            results.isEmpty() -> item { EmptyMessage("No matches", "Try a different company or ticker.") }
            else -> items(results, key = { it.symbol }) { quote -> QuoteRow(quote, { onStock(quote.symbol) }, Modifier.padding(horizontal = 20.dp)) }
        }
    }
}

@Composable
fun WatchlistScreen(quotes: List<Quote>, onStock: (String) -> Unit, onDiscover: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(AlphaColors.Background)) {
        item { ScreenHeader("Curated by you", "Watchlist") }
        if (quotes.isEmpty()) {
            item {
                EmptyMessage("Your watchlist is empty", "Save companies to keep their movement close.")
                AlphaButton("Discover stocks", onDiscover, Modifier.fillMaxWidth().padding(horizontal = 20.dp))
            }
        } else items(quotes, key = { it.symbol }) { quote ->
            QuoteRow(quote, { onStock(quote.symbol) }, Modifier.padding(horizontal = 20.dp))
            HorizontalDivider(Modifier.padding(horizontal = 78.dp), color = AlphaColors.Line.copy(alpha = .55f))
        }
    }
}

@Composable
fun PortfolioScreen(state: PocketAlphaState, onStock: (String) -> Unit, onLogout: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(AlphaColors.Background)) {
        item {
            ScreenHeader("Paper investing", "Portfolio") {
                IconButton(onClick = onLogout) { Icon(Icons.Rounded.Logout, "Sign out", tint = AlphaColors.Muted) }
            }
            PortfolioHero(state.portfolio)
            Spacer(Modifier.height(28.dp))
            Text("Positions", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
        }
        if (state.portfolio.positions.isEmpty()) item { EmptyMessage("No positions yet", "Open a company and place a paper order.") }
        else items(state.portfolio.positions, key = { it.symbol }) { position ->
            Row(Modifier.fillMaxWidth().clickable { onStock(position.symbol) }.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(position.symbol, fontWeight = FontWeight.SemiBold)
                    Text("${quantity(position.quantity)} shares · ${money(position.averageCost)} avg", color = AlphaColors.Muted, style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(money(position.marketValue), fontWeight = FontWeight.Medium)
                    Text(percent(position.gainLossPercent), color = if (position.gainLoss >= 0) AlphaColors.Green else AlphaColors.Coral, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            Text("Recent activity", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        }
        if (state.portfolio.orders.isEmpty()) item { Text("Your completed paper orders will appear here.", color = AlphaColors.Muted, modifier = Modifier.padding(20.dp)) }
        else items(state.portfolio.orders.take(8), key = { it.id }) { order ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).background(if (order.side == "BUY") AlphaColors.Green.copy(alpha = .14f) else AlphaColors.Coral.copy(alpha = .14f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = if (order.side == "BUY") AlphaColors.Green else AlphaColors.Coral, modifier = Modifier.size(19.dp))
                }
                Column(Modifier.padding(start = 12.dp).weight(1f)) { Text("${order.side.lowercase().replaceFirstChar { it.uppercase() }} ${order.symbol}", fontWeight = FontWeight.Medium); Text("${quantity(order.quantity)} shares at ${money(order.price)}", color = AlphaColors.Muted, style = MaterialTheme.typography.bodyMedium) }
                Text(money(order.total), fontWeight = FontWeight.Medium)
            }
        }
        item { Spacer(Modifier.height(26.dp)); Box(Modifier.padding(horizontal = 20.dp)) { DisclosureCard() }; Spacer(Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(state: PocketAlphaState, viewModel: AppViewModel) {
    val history = state.selected ?: return
    val quote = history.quote
    val saved = state.watchlist.any { it.symbol == quote.symbol }
    var tradeSide by remember { mutableStateOf<String?>(null) }
    val positive = quote.changePercent >= 0

    Column(Modifier.fillMaxSize().background(AlphaColors.Background).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = viewModel::closeStock) { Icon(Icons.Rounded.ArrowBack, "Back") }
            Column(Modifier.weight(1f).padding(start = 6.dp)) { Text(quote.symbol, fontWeight = FontWeight.SemiBold); Text(quote.name, color = AlphaColors.Muted, style = MaterialTheme.typography.bodyMedium) }
            IconButton(onClick = { viewModel.toggleWatchlist(quote.symbol) }) { Icon(if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, if (saved) "Remove from watchlist" else "Add to watchlist", tint = if (saved) AlphaColors.Green else AlphaColors.Text) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(24.dp))
            Text(money(quote.price), style = MaterialTheme.typography.displaySmall)
            Text("${if (quote.change >= 0) "+" else ""}${money(quote.change)}  ${percent(quote.changePercent)}", color = if (positive) AlphaColors.Green else AlphaColors.Coral, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 5.dp))
            Text(if (quote.isMarketOpen) "Market open · simulated" else "Market closed · simulated", color = AlphaColors.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 5.dp))
            PriceChart(history.points, positive, Modifier.fillMaxWidth().height(230.dp).padding(vertical = 24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("1D", "1W", "1M", "3M", "1Y").forEach { range ->
                    Text(
                        range,
                        color = if (state.selectedRange == range) AlphaColors.Background else AlphaColors.Muted,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (state.selectedRange == range) AlphaColors.Green else Color.Transparent).clickable { viewModel.selectStock(quote.symbol, range) }.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            SectionHeading("Company snapshot")
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill("Previous close", money(quote.previousClose), modifier = Modifier.weight(1f))
                StatPill("Market cap", compactMarketCap(quote.marketCap), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill("Sector", quote.sector, modifier = Modifier.weight(1f))
                StatPill("Day move", percent(quote.changePercent), positive, Modifier.weight(1f))
            }
            Spacer(Modifier.height(28.dp))
            DisclosureCard()
            Spacer(Modifier.height(24.dp))
        }
        Row(Modifier.fillMaxWidth().background(AlphaColors.Surface).navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AlphaButton("Sell", { tradeSide = "SELL" }, Modifier.weight(1f))
            AlphaButton("Buy", { tradeSide = "BUY" }, Modifier.weight(1f))
        }
    }
    tradeSide?.let { side ->
        TradeSheet(quote, side, state.portfolio, state.loading, onDismiss = { tradeSide = null }) { amount ->
            viewModel.placeOrder(quote.symbol, side, amount) { tradeSide = null }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TradeSheet(quote: Quote, side: String, portfolio: Portfolio, loading: Boolean, onDismiss: () -> Unit, onSubmit: (Double) -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }
    val shares = amount.toDoubleOrNull() ?: 0.0
    val estimated = shares * quote.price
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = AlphaColors.Surface, dragHandle = null) {
        Column(Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("$side ${quote.symbol}", style = MaterialTheme.typography.headlineMedium); Text("Paper order at approximately ${money(quote.price)}", color = AlphaColors.Muted, style = MaterialTheme.typography.bodyMedium) }
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Close") }
            }
            Spacer(Modifier.height(22.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { candidate -> if (candidate.all { it.isDigit() || it == '.' }) amount = candidate },
                label = { Text("Number of shares") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AlphaColors.Green, unfocusedBorderColor = AlphaColors.Line),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Estimated total", color = AlphaColors.Muted); Text(money(estimated), fontWeight = FontWeight.SemiBold) }
            Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(if (side == "BUY") "Buying power" else "Shares owned", color = AlphaColors.Muted); Text(if (side == "BUY") money(portfolio.cash) else quantity(portfolio.positions.find { it.symbol == quote.symbol }?.quantity ?: 0.0)) }
            AlphaButton("Review $side order", { onSubmit(shares) }, Modifier.fillMaxWidth(), loading = loading, enabled = shares > 0)
        }
    }
}

fun compactMarketCap(value: Long): String = when {
    value >= 1_000_000_000_000 -> "%.2fT".format(value / 1_000_000_000_000.0)
    value >= 1_000_000_000 -> "%.1fB".format(value / 1_000_000_000.0)
    else -> "%.1fM".format(value / 1_000_000.0)
}
