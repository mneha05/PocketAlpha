package com.nehamahesh.pocketalpha

data class User(val id: Int, val name: String, val email: String)

data class Quote(
    val symbol: String,
    val name: String,
    val price: Double,
    val previousClose: Double,
    val change: Double,
    val changePercent: Double,
    val sector: String,
    val marketCap: Long,
    val isMarketOpen: Boolean
)

data class IndexSnapshot(val symbol: String, val price: Double, val changePercent: Double)
data class HistoryPoint(val timestamp: String, val value: Double)

data class MarketOverview(
    val indices: List<IndexSnapshot>,
    val movers: List<Quote>,
    val asOf: String,
    val source: String
)

data class Position(
    val symbol: String,
    val quantity: Double,
    val averageCost: Double,
    val currentPrice: Double,
    val marketValue: Double,
    val gainLoss: Double,
    val gainLossPercent: Double
)

data class PaperOrder(
    val id: Int,
    val symbol: String,
    val side: String,
    val quantity: Double,
    val price: Double,
    val total: Double,
    val createdAt: String
)

data class Portfolio(
    val cash: Double = 0.0,
    val holdingsValue: Double = 0.0,
    val totalValue: Double = 0.0,
    val positions: List<Position> = emptyList(),
    val orders: List<PaperOrder> = emptyList()
)

data class AuthResult(val token: String, val user: User)
data class StockHistory(val quote: Quote, val range: String, val points: List<HistoryPoint>)

class ApiException(message: String, val statusCode: Int) : Exception(message)
