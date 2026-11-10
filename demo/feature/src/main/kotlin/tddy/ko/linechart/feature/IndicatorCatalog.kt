package tddy.ko.linechart.feature

import androidx.compose.ui.graphics.Color
import tddy.ko.linechart.ui.Atr
import tddy.ko.linechart.ui.BollingerBands
import tddy.ko.linechart.ui.Cci
import tddy.ko.linechart.ui.Disparity
import tddy.ko.linechart.ui.Dmi
import tddy.ko.linechart.ui.Envelope
import tddy.ko.linechart.ui.Ichimoku
import tddy.ko.linechart.ui.Indicator
import tddy.ko.linechart.ui.Macd
import tddy.ko.linechart.ui.MacdOscillator
import tddy.ko.linechart.ui.Mfi
import tddy.ko.linechart.ui.Momentum
import tddy.ko.linechart.ui.MovingAverage
import tddy.ko.linechart.ui.Obv
import tddy.ko.linechart.ui.ParabolicSar
import tddy.ko.linechart.ui.Pivot
import tddy.ko.linechart.ui.PriceChannel
import tddy.ko.linechart.ui.Psychological
import tddy.ko.linechart.ui.Roc
import tddy.ko.linechart.ui.Rsi
import tddy.ko.linechart.ui.Sonar
import tddy.ko.linechart.ui.StochasticFast
import tddy.ko.linechart.ui.StochasticSlow
import tddy.ko.linechart.ui.TradingValue
import tddy.ko.linechart.ui.Trix
import tddy.ko.linechart.ui.Vwap
import tddy.ko.linechart.ui.VolumeOscillator
import tddy.ko.linechart.ui.VolumeProfile
import tddy.ko.linechart.ui.VolumeRatio
import tddy.ko.linechart.ui.Volume
import tddy.ko.linechart.ui.WilliamsR
import kotlin.math.roundToLong

class Param(val label: String, val default: Double, val min: Double, val max: Double, val step: Double) {
    fun next(value: Double, direction: Int): Double =
        ((value + direction * step).coerceIn(min, max) * 1000).roundToLong() / 1000.0
}

class Args(private val values: List<Double>, private val color: Color?) {
    fun i(index: Int): Int = values[index].toInt()
    fun d(index: Int): Double = values[index]
    fun paint(base: List<Color>): List<Color> = color?.let { listOf(it) + base.drop(1) } ?: base
}

class IndicatorSpec(
    val key: String,
    val name: String,
    val overlay: Boolean,
    val params: List<Param>,
    val colorable: Boolean,
    val build: (Args) -> Indicator,
) {
    fun entry(): Entry = Entry(key, params.map { it.default })
}

data class Entry(val key: String, val values: List<Double>, val color: Color? = null) {
    val spec: IndicatorSpec get() = Catalog.spec(key)

    fun build(): Indicator = spec.build(Args(values, color))

    fun with(values: List<Double> = this.values, color: Color? = this.color): Entry = copy(values = values, color = color)
}

fun formatParam(value: Double): String = value.toBigDecimal().stripTrailingZeros().toPlainString()

private fun count(label: String, default: Int, max: Int = 200) = Param(label, default.toDouble(), 1.0, max.toDouble(), 1.0)

private fun spec(key: String, name: String, overlay: Boolean, params: List<Param> = emptyList(), colorable: Boolean = true, build: (Args) -> Indicator) =
    IndicatorSpec(key, name, overlay, params, colorable, build)

object Catalog {
    val overlays: List<IndicatorSpec> = listOf(
        spec("ma", "이동평균선", true, colorable = false) { MovingAverage() },
        spec("bb", "볼린저밴드", true, listOf(count("기간", 20), Param("승수", 2.0, 0.5, 5.0, 0.5))) { BollingerBands(it.i(0), it.d(1), it.paint(BollingerBands().colors)) },
        spec("env", "엔벨로프", true, listOf(count("기간", 20), Param("%", 6.0, 1.0, 30.0, 1.0))) { Envelope(it.i(0), it.d(1), it.paint(Envelope().colors)) },
        spec("ichi", "일목균형표", true, listOf(count("전환", 9), count("기준", 26), count("선행", 52), count("이동", 26, 60))) {
            Ichimoku(it.i(0), it.i(1), it.i(2), it.i(3), it.paint(Ichimoku().colors))
        },
        spec("sar", "파라볼릭 SAR", true, listOf(Param("가속", 0.02, 0.01, 0.1, 0.01), Param("최대", 0.2, 0.1, 1.0, 0.1)), colorable = false) { ParabolicSar(it.d(0), it.d(1)) },
        spec("pc", "가격채널", true, listOf(count("기간", 20))) { PriceChannel(it.i(0), it.paint(PriceChannel().colors)) },
        spec("pivot", "피봇", true, colorable = false) { Pivot() },
        spec("vwap", "VWAP", true) { Vwap(it.paint(Vwap().colors)) },
        spec("vp", "매물대", true, listOf(Param("구간", 10.0, 4.0, 40.0, 2.0)), colorable = false) { VolumeProfile(it.i(0)) },
    )

    val panes: List<IndicatorSpec> = listOf(
        spec("vol", "거래량", false, listOf(count("MA1", 5), count("MA2", 20))) { Volume(listOf(it.i(0), it.i(1)), it.paint(Volume().maColors)) },
        spec("tv", "거래대금", false, listOf(count("MA1", 5), count("MA2", 20))) { TradingValue(listOf(it.i(0), it.i(1)), it.paint(TradingValue().maColors)) },
        spec("macd", "MACD", false, listOf(count("단기", 12), count("장기", 26), count("시그널", 9))) { Macd(it.i(0), it.i(1), it.i(2), it.paint(Macd().colors)) },
        spec("macdo", "MACD 오실레이터", false, listOf(count("단기", 12), count("장기", 26), count("시그널", 9)), colorable = false) { MacdOscillator(it.i(0), it.i(1), it.i(2)) },
        spec("rsi", "RSI", false, listOf(count("기간", 14), count("시그널", 6))) { Rsi(it.i(0), it.i(1), it.paint(Rsi().colors)) },
        spec("sf", "스토캐스틱 Fast", false, listOf(count("%K", 5), count("%D", 3))) { StochasticFast(it.i(0), it.i(1), it.paint(StochasticFast().colors)) },
        spec("ss", "스토캐스틱 Slow", false, listOf(count("%K", 5), count("스무딩", 3), count("%D", 3))) { StochasticSlow(it.i(0), it.i(1), it.i(2), it.paint(StochasticSlow().colors)) },
        spec("wr", "Williams %R", false, listOf(count("기간", 14))) { WilliamsR(it.i(0), it.paint(WilliamsR().colors)) },
        spec("cci", "CCI", false, listOf(count("기간", 20))) { Cci(it.i(0), it.paint(Cci().colors)) },
        spec("dmi", "DMI", false, listOf(count("기간", 14), count("ADX", 14)), colorable = false) { Dmi(it.i(0), it.i(1)) },
        spec("atr", "ATR", false, listOf(count("기간", 14))) { Atr(it.i(0), it.paint(Atr().colors)) },
        spec("obv", "OBV", false, listOf(count("시그널", 9))) { Obv(it.i(0), it.paint(Obv().colors)) },
        spec("mfi", "MFI", false, listOf(count("기간", 14))) { Mfi(it.i(0), it.paint(Mfi().colors)) },
        spec("mom", "Momentum", false, listOf(count("기간", 10), count("시그널", 9))) { Momentum(it.i(0), it.i(1), it.paint(Momentum().colors)) },
        spec("roc", "ROC", false, listOf(count("기간", 12))) { Roc(it.i(0), it.paint(Roc().colors)) },
        spec("trix", "TRIX", false, listOf(count("기간", 12), count("시그널", 9))) { Trix(it.i(0), it.i(1), it.paint(Trix().colors)) },
        spec("disp", "이격도", false, listOf(count("기간1", 20), count("기간2", 60))) { Disparity(listOf(it.i(0), it.i(1)), it.paint(Disparity().colors)) },
        spec("psy", "심리도", false, listOf(count("기간", 10))) { Psychological(it.i(0), it.paint(Psychological().colors)) },
        spec("vr", "거래량비율", false, listOf(count("기간", 20))) { VolumeRatio(it.i(0), it.paint(VolumeRatio().colors)) },
        spec("vo", "거래량 오실레이터", false, listOf(count("단기", 5), count("장기", 20))) { VolumeOscillator(it.i(0), it.i(1), it.paint(VolumeOscillator().colors)) },
        spec("sonar", "Sonar", false, listOf(count("기간", 20), count("래그", 9), count("시그널", 9))) { Sonar(it.i(0), it.i(1), it.i(2), it.paint(Sonar().colors)) },
    )

    private val all = (overlays + panes).associateBy { it.key }

    fun spec(key: String): IndicatorSpec = all.getValue(key)
}
