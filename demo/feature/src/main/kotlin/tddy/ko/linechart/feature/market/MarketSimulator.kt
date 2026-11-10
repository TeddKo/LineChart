package tddy.ko.linechart.feature.market

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import tddy.ko.linechart.orderbook.FeePolicy
import tddy.ko.linechart.orderbook.MatchingEngine
import tddy.ko.linechart.orderbook.MatchingPolicy
import tddy.ko.linechart.orderbook.OrderResult
import tddy.ko.linechart.orderbook.OrderType
import tddy.ko.linechart.orderbook.Side
import tddy.ko.linechart.ui.ChartData
import tddy.ko.linechart.ui.MarketLevel
import tddy.ko.linechart.ui.MarketOption
import tddy.ko.linechart.ui.MarketTrade
import tddy.ko.linechart.ui.toDailyChartData
import kotlin.random.Random

const val TICK_SIZE = 1_000L
const val OWNER = "me"
val ExpiryDays = listOf(1, 3, 7, 30, 60)
val Options = (230..290 step 5).map { it.toString() }

private const val DAY_MILLIS = 86_400_000L
private const val HISTORY_DAYS = 400
private const val HISTORY_STEP_MILLIS = 6 * 3_600_000L
private const val LIVE_MILLIS = 500L
private const val TRADE_LIMIT = 1_000
private const val FEE_CAP = 300_000L

val DemoFee = FeePolicy { side, price, quantity ->
    minOf(price * quantity * (if (side == Side.Buy) 3 else 6) / 100, FEE_CAP)
}

class MarketUiState(
    val options: List<MarketOption> = emptyList(),
    val selected: String = Options.first(),
    val bids: List<MarketLevel> = emptyList(),
    val asks: List<MarketLevel> = emptyList(),
    val trades: List<MarketTrade> = emptyList(),
    val history: List<ChartData> = emptyList(),
    val now: Long = 0L,
) {
    val lastPrice: Long? get() = trades.firstOrNull { it.option == selected }?.price
    val bestAsk: Long? get() = asks.firstOrNull()?.price
    val bestBid: Long? get() = bids.firstOrNull()?.price
}

/**
 * Demo market over [MatchingEngine] with an injected clock: seeded history, then a live loop of random orders.
 * Every engine access runs on the caller's thread, and the seeding yields between steps so the screen stays responsive.
 */
class MarketSimulator(private val random: Random = Random(11)) {
    private var now = System.currentTimeMillis()
    private val engine = MatchingEngine(MatchingPolicy(tickSize = TICK_SIZE, minPrice = TICK_SIZE), DemoFee, clock = { now })
    private val anchor = Options.associateWith { 150_000L + (it.toLong() - 230) * 1_000L }
    private val fair = anchor.toMutableMap()
    private val all = ArrayList<MarketTrade>()
    private val resting = ArrayDeque<Long>()
    private var ready = false
    private var historyOption = ""
    private var historyCount = -1
    private var history = emptyList<ChartData>()

    var ui by mutableStateOf(MarketUiState())
        private set

    private var selected = Options.first()

    suspend fun run() {
        if (!ready) {
            seed()
            ready = true
            publish()
        }
        while (true) {
            delay(LIVE_MILLIS)
            now = System.currentTimeMillis()
            step(Options.random(random))
            engine.expire(now)
            publish()
        }
    }

    fun select(option: String) {
        selected = option
        publish()
    }

    fun instant(side: Side, bound: Long?): OrderResult = engine.place(selected, side, OrderType.Instant, 1, bound, owner = OWNER).also { record(it); publish() }

    fun limit(side: Side, price: Long, days: Int): OrderResult =
        engine.place(selected, side, OrderType.Limit, 1, price, now + days * DAY_MILLIS, OWNER).also { record(it); publish() }

    private suspend fun seed() {
        val start = now - HISTORY_DAYS * DAY_MILLIS
        val end = now
        var time = start
        while (time < end) {
            now = time
            Options.forEach { step(it) }
            time += HISTORY_STEP_MILLIS
            yield()
        }
        now = end
    }

    private fun step(option: String) {
        val base = anchor.getValue(option)
        val walked = (fair.getValue(option) + random.nextInt(-3, 4) * TICK_SIZE).coerceIn(base - 40_000L, base + 40_000L)
        fair[option] = walked
        val days = ExpiryDays.random(random)
        record(engine.place(option, Side.Buy, OrderType.Limit, random.nextInt(1, 4), walked - random.nextInt(1, 6) * TICK_SIZE, now + days * DAY_MILLIS))
        record(engine.place(option, Side.Sell, OrderType.Limit, random.nextInt(1, 4), walked + random.nextInt(1, 6) * TICK_SIZE, now + days * DAY_MILLIS))
        if (random.nextInt(100) < 60) record(engine.place(option, if (random.nextBoolean()) Side.Buy else Side.Sell, OrderType.Instant, 1))
    }

    private fun record(result: OrderResult) {
        result.fills.forEach { all += MarketTrade(it.option, it.price, it.quantity, it.time) }
        result.orderId?.takeIf { result.restingQuantity > 0 }?.let {
            resting += it
            if (resting.size > 200) engine.cancel(resting.removeFirst())
        }
    }

    private fun publish() {
        if (!ready) return
        val book = engine.snapshot(selected, 20)
        val own = all.count { it.option == selected }
        if (historyOption != selected || historyCount != own) {
            history = all.filter { it.option == selected }.toDailyChartData()
            historyOption = selected
            historyCount = own
        }
        ui = MarketUiState(
            options = Options.map { engine.snapshot(it, 1).let { s -> MarketOption(it, it, s.bestAsk, s.bestBid) } },
            selected = selected,
            bids = book.bids.map { MarketLevel(it.price, it.quantity) },
            asks = book.asks.map { MarketLevel(it.price, it.quantity) },
            trades = all.asReversed().take(TRADE_LIMIT),
            history = history,
            now = now,
        )
    }
}
