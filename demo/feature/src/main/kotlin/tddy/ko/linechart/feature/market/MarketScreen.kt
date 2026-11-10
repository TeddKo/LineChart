package tddy.ko.linechart.feature.market

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tddy.ko.linechart.feature.Segmented
import tddy.ko.linechart.feature.Stepper
import tddy.ko.linechart.orderbook.OrderResult
import tddy.ko.linechart.orderbook.Side
import tddy.ko.linechart.ui.ChartColors
import tddy.ko.linechart.ui.ChartSettings
import tddy.ko.linechart.ui.ChartType
import tddy.ko.linechart.ui.DepthChart
import tddy.ko.linechart.ui.InstantTradeButtons
import tddy.ko.linechart.ui.LineChartPanel
import tddy.ko.linechart.ui.MarketSide
import tddy.ko.linechart.ui.OptionSelector
import tddy.ko.linechart.ui.OrderBookLadder
import tddy.ko.linechart.ui.Timeframe
import tddy.ko.linechart.ui.TimeframeUnit
import tddy.ko.linechart.ui.TradeList
import tddy.ko.linechart.ui.aggregate
import tddy.ko.linechart.ui.rememberLineChartState
import java.util.Locale

private const val DAY_MILLIS = 86_400_000L

private enum class Period(val label: String, val days: Int, val unit: TimeframeUnit) {
    Month1("1개월", 30, TimeframeUnit.Day),
    Month3("3개월", 90, TimeframeUnit.Day),
    Month6("6개월", 180, TimeframeUnit.Week),
    Year1("1년", 365, TimeframeUnit.Week),
    All("전체", Int.MAX_VALUE, TimeframeUnit.Month),
}

private fun money(value: Long): String = String.format(Locale.ROOT, "%,d", value)

private fun OrderResult.describe(): String = when {
    orderId == null -> "거부: $reason"
    filledQuantity > 0 && restingQuantity > 0 -> "일부 체결 ${filledQuantity}개, ${restingQuantity}개 대기"
    filledQuantity > 0 -> "체결 완료 ${filledQuantity}개 @ ${money(fills.first().price)}"
    restingQuantity > 0 -> "입찰 등록 (대기)"
    else -> "취소됨: $reason"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(sim: MarketSimulator, colors: ChartColors) {
    LaunchedEffect(sim) { sim.run() }
    val ui = sim.ui
    var period by rememberSaveable { mutableStateOf(Period.Month3) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sheet by remember { mutableStateOf(false) }
    var sheetSide by remember { mutableStateOf(Side.Buy) }
    var sheetPrice by remember { mutableLongStateOf(0L) }
    var message by remember { mutableStateOf("") }

    fun openSheet(side: Side, price: Long?) {
        sheetSide = side
        sheetPrice = price ?: ui.lastPrice ?: TICK_SIZE
        sheet = true
    }

    val items = remember(ui.history, period) {
        val from = if (period.days == Int.MAX_VALUE) Long.MIN_VALUE else System.currentTimeMillis() - period.days * DAY_MILLIS
        ui.history.filter { it.date >= from }.aggregate(Timeframe(period.unit, 1))
    }
    val state = rememberLineChartState(items)

    Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("DEMO 한정판 ${ui.selected}", style = MaterialTheme.typography.titleMedium)
            Text(ui.lastPrice?.let { money(it) } ?: "-", style = MaterialTheme.typography.headlineMedium)
        }
        OptionSelector(ui.options, ui.selected, sim::select, colors = colors)
        InstantTradeButtons(
            bestAsk = ui.bestAsk,
            bestBid = ui.bestBid,
            onBuy = { message = sim.instant(Side.Buy, ui.bestAsk).describe() },
            onSell = { message = sim.instant(Side.Sell, ui.bestBid).describe() },
            colors = colors,
        )
        if (message.isNotEmpty()) Text(message, Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Period.entries.forEach { FilterChip(selected = period == it, onClick = { period = it }, label = { Text(it.label) }) }
        }
        LineChartPanel(
            Modifier.padding(horizontal = 16.dp),
            chartHeight = 220.dp,
            state = state,
            chartType = ChartType.Area,
            colors = colors,
            overlays = emptyList(),
            panes = emptyList(),
            settings = ChartSettings(),
            timeframe = Timeframe(period.unit, 1),
            priceFormatter = { money(it.toLong()) },
        )
        DepthChart(ui.bids, ui.asks, Modifier.padding(horizontal = 16.dp), colors = colors)
        PrimaryTabRow(selectedTabIndex = tab) {
            listOf("체결 거래", "판매 입찰", "구매 입찰").forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
            }
        }
        when (tab) {
            0 -> TradeList(ui.trades, colors = colors)
            1 -> OrderBookLadder(ui.asks, MarketSide.Ask, onLevelClick = { openSheet(Side.Buy, it) }, colors = colors)
            else -> OrderBookLadder(ui.bids, MarketSide.Bid, onLevelClick = { openSheet(Side.Sell, it) }, colors = colors)
        }
        Button({ openSheet(Side.Buy, ui.bestBid) }, Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { Text("입찰하기") }
    }

    if (sheet) {
        BidSheet(sheetSide, sheetPrice, onSide = { sheetSide = it }, onPrice = { sheetPrice = it }, onSubmit = { side, price, days -> sim.limit(side, price, days).describe() }, onDismiss = { sheet = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BidSheet(
    side: Side,
    price: Long,
    onSide: (Side) -> Unit,
    onPrice: (Long) -> Unit,
    onSubmit: (Side, Long, Int) -> String,
    onDismiss: () -> Unit,
    sheetState: SheetState,
) {
    var days by remember { mutableIntStateOf(30) }
    var result by remember { mutableStateOf("") }
    val fee = DemoFee.fee(side, price, 1)
    val total = if (side == Side.Buy) price + fee else price - fee
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("입찰하기", style = MaterialTheme.typography.titleLarge)
            Segmented(listOf("구매 입찰" to Side.Buy, "판매 입찰" to Side.Sell), side) { onSide(it); result = "" }
            Stepper("가격", money(price), { onPrice((price - TICK_SIZE).coerceAtLeast(TICK_SIZE)) }, { onPrice(price + TICK_SIZE) })
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExpiryDays.forEach { FilterChip(selected = days == it, onClick = { days = it }, label = { Text("${it}일") }) }
            }
            Text("수수료 ${money(fee)}원")
            Text(if (side == Side.Buy) "결제 금액 ${money(total)}원" else "정산 금액 ${money(total)}원", style = MaterialTheme.typography.titleMedium)
            Button({ result = onSubmit(side, price, days) }, Modifier.fillMaxWidth()) { Text("입찰 등록") }
            if (result.isNotEmpty()) Text(result, color = MaterialTheme.colorScheme.primary)
        }
    }
}
