package tddy.ko.linechart.feature

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tddy.ko.linechart.ui.AxisSide
import tddy.ko.linechart.ui.BaseLine
import tddy.ko.linechart.ui.ChartType
import tddy.ko.linechart.ui.GridLines
import tddy.ko.linechart.ui.MaType
import tddy.ko.linechart.ui.PriceScale
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: DemoViewModel, onBack: () -> Unit) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    Scaffold(topBar = {
        TopAppBar(title = { Text("설정") }, navigationIcon = { TextButton(onBack) { Text("‹ 뒤로") } })
    }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChartSection(vm)
            PriceSection(vm)
            DisplaySection(vm)
            MovingAverageSection(vm)
            IndicatorSection(vm)
            SectionTitle("초기화")
            Button({ confirmReset = true }, Modifier.fillMaxWidth()) { Text("모든 설정 초기화") }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("초기화") },
            text = { Text("모든 설정과 지표를 기본값으로 되돌립니다.") },
            confirmButton = { TextButton({ vm.reset(); confirmReset = false }) { Text("초기화") } },
            dismissButton = { TextButton({ confirmReset = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun ChartSection(vm: DemoViewModel) {
    val p = vm.prefs
    SectionTitle("차트")
    Segmented(listOf("캔들" to ChartType.Candle, "라인" to ChartType.Line, "영역" to ChartType.Area), p.chartType) { vm.prefs = p.copy(chartType = it) }
    Segmented(listOf("한국식" to false, "국제식" to true), p.international) { vm.prefs = p.copy(international = it, up = null, down = null) }
    SwatchRow("상승 색", p.up) { vm.prefs = p.copy(up = it) }
    SwatchRow("하락 색", p.down) { vm.prefs = p.copy(down = it) }
    Segmented(ThemeMode.entries.map { it.label to it }, p.theme) { vm.prefs = p.copy(theme = it) }
    Stepper("글자 크기", String.format(Locale.ROOT, "×%.1f", p.fontScale), { vm.prefs = p.copy(fontScale = (p.fontScale - 0.1f).coerceAtLeast(0.7f)) }, { vm.prefs = p.copy(fontScale = (p.fontScale + 0.1f).coerceAtMost(1.6f)) })
}

@Composable
private fun PriceSection(vm: DemoViewModel) {
    val p = vm.prefs
    SectionTitle("가격·축")
    Segmented(listOf("일반" to PriceScale.Linear, "로그" to PriceScale.Log, "퍼센트" to PriceScale.Percent), p.scale) { vm.prefs = p.copy(scale = it) }
    Segmented(listOf("왼쪽 축" to AxisSide.Left, "오른쪽 축" to AxisSide.Right), p.axisSide) { vm.prefs = p.copy(axisSide = it) }
    Stepper("우측 여백(칸)", p.rightMargin.toString(), { vm.prefs = p.copy(rightMargin = (p.rightMargin - 1).coerceAtLeast(0)) }, { vm.prefs = p.copy(rightMargin = (p.rightMargin + 1).coerceAtMost(60)) })
    SwitchRow("수정주가 (일봉 이상)", p.adjusted) { vm.prefs = p.copy(adjusted = it) }
}

@Composable
private fun DisplaySection(vm: DemoViewModel) {
    val p = vm.prefs
    SectionTitle("표시")
    Segmented(listOf("없음" to GridLines.None, "가로" to GridLines.Horizontal, "세로" to GridLines.Vertical, "모두" to GridLines.Both), p.grid) { vm.prefs = p.copy(grid = it) }
    SwitchRow("크로스헤어", p.crosshair) { vm.prefs = p.copy(crosshair = it) }
    SwitchRow("범례", p.legend) { vm.prefs = p.copy(legend = it) }
    SwitchRow("현재가", p.currentPrice) { vm.prefs = p.copy(currentPrice = it) }
    Text("기준선")
    Segmented(listOf("없음" to BaseLine.None, "전일종가" to BaseLine.PreviousClose, "당일시가" to BaseLine.TodayOpen, "피봇" to BaseLine.Pivot), p.baseLine) { vm.prefs = p.copy(baseLine = it) }
    SwitchRow("최고·최저 표시", p.highLow) { vm.prefs = p.copy(highLow = it) }
    SwitchRow("상·하한가", p.priceLimits) { vm.prefs = p.copy(priceLimits = it) }
    Stepper("상·하한 폭(%)", formatParam(p.limitPercent), { vm.prefs = p.copy(limitPercent = (p.limitPercent - 1).coerceAtLeast(1.0)) }, { vm.prefs = p.copy(limitPercent = (p.limitPercent + 1).coerceAtMost(30.0)) })
    SwitchRow("거래량 이동평균", p.volumeMa) { vm.prefs = p.copy(volumeMa = it) }
}

@Composable
private fun MovingAverageSection(vm: DemoViewModel) {
    SectionTitle("이동평균선")
    vm.maLines.forEachIndexed { index, line ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("선 ${index + 1}", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    TextButton({ vm.maLines = vm.maLines.filterIndexed { i, _ -> i != index } }) { Text("삭제") }
                }
                Stepper("기간", line.period.toString(), { vm.editMa(index) { it.copy(period = (it.period - 1).coerceAtLeast(1)) } }, { vm.editMa(index) { it.copy(period = (it.period + 1).coerceAtMost(500)) } })
                Segmented(listOf("단순" to MaType.Simple, "지수" to MaType.Exponential, "가중" to MaType.Weighted), line.type) { vm.editMa(index) { old -> old.copy(type = it) } }
                SwatchRow("색", line.color) { color -> vm.editMa(index) { it.copy(color = color ?: it.color) } }
                Stepper("굵기", String.format(Locale.ROOT, "%.1f", line.width), { vm.editMa(index) { it.copy(width = (it.width - 0.5f).coerceAtLeast(0.5f)) } }, { vm.editMa(index) { it.copy(width = (it.width + 0.5f).coerceAtMost(5f)) } })
                SwitchRow("표시", line.visible) { visible -> vm.editMa(index) { it.copy(visible = visible) } }
            }
        }
    }
    FilledTonalButton({ vm.maLines = vm.maLines + MaLine(10, MaType.Simple, Swatches[vm.maLines.size % Swatches.size], 1f, true) }) { Text("＋ 선 추가") }
}

@Composable
private fun IndicatorSection(vm: DemoViewModel) {
    SectionTitle("보조지표")
    IndicatorList("상단 지표", true, vm)
    IndicatorList("하단 지표", false, vm)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IndicatorList(title: String, overlay: Boolean, vm: DemoViewModel) {
    val entries = vm.entries(overlay)
    Text(title, style = MaterialTheme.typography.titleSmall)
    entries.forEachIndexed { index, entry ->
        val spec = entry.spec
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(spec.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    TextButton({ vm.move(overlay, index, -1) }, contentPadding = PaddingValues(8.dp)) { Text("▲") }
                    TextButton({ vm.move(overlay, index, 1) }, contentPadding = PaddingValues(8.dp)) { Text("▼") }
                    TextButton({ vm.remove(overlay, index) }, contentPadding = PaddingValues(8.dp)) { Text("✕") }
                }
                if (spec.key == "ma") Text("이동평균선 섹션에서 편집", style = MaterialTheme.typography.bodySmall)
                spec.params.forEachIndexed { k, param ->
                    Stepper(param.label, formatParam(entry.values[k]), { vm.change(overlay, index, k, param, -1) }, { vm.change(overlay, index, k, param, 1) })
                }
                if (spec.colorable) SwatchRow("색", entry.color) { vm.edit(overlay, index, entry.with(color = it)) }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
    val rest = (if (overlay) Catalog.overlays else Catalog.panes).filterNot { vm.isOn(it) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        rest.forEach { spec -> FilledTonalButton({ vm.toggle(spec, true) }, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("＋ ${spec.name}") } }
    }
}

private fun DemoViewModel.change(overlay: Boolean, index: Int, k: Int, param: Param, direction: Int) {
    val entry = entries(overlay)[index]
    val next = entry.with(values = entry.values.mapIndexed { i, v -> if (i == k) param.next(v, direction) else v })
    if (runCatching { next.build() }.isSuccess) edit(overlay, index, next)
}
