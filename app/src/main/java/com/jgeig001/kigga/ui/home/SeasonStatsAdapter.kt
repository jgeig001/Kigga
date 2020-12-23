package com.jgeig001.kigga.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.jgeig001.kigga.R
import com.jgeig001.kigga.databinding.SeasonStatsBinding
import com.jgeig001.kigga.model.domain.BetPoints
import com.jgeig001.kigga.model.domain.Matchday
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.model.domain.Season
import com.jgeig001.kigga.utils.FloatRounder.round2D
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.pie_chart.*
import kotlinx.android.synthetic.main.season_stats.*
import kotlinx.android.synthetic.main.season_stats.view.*
import kotlin.math.roundToInt

class LineValueFomatter(private val maxValue: Float) : ValueFormatter() {
    /** only show the peak value */
    override fun getFormattedValue(value: Float): String {
        return if (value == maxValue) value.toInt().toString() else ""
    }
}

class SeasonStatsViewHolder(var binding: SeasonStatsBinding, private var context: Context) :
    RecyclerView.ViewHolder(binding.root) {

    private lateinit var thisSeason: Season

    private val black_n_light_COLOR = context.resources.getColor(R.color.black_n_light)
    private val black_n_light_disbaled_COLOR = context.resources.getColor(R.color.black_n_light_disabled)

    fun setThisSeason(season: Season) {
        thisSeason = season
    }

    fun buildStatsGraph() {

        val chart = binding.statsGraph

        chart.setDrawOrder(arrayOf(CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.LINE))
        chart.getDescription().isEnabled = false
        chart.onTouchListener = null
        chart.extraBottomOffset = 5f

        chart.getAxisLeft().apply {
            setDrawGridLines(true)
            axisMinimum = 0f // this replaces setStartAtZero(true)
            textColor = black_n_light_COLOR
            textSize = 13f
        }

        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false

        chart.getXAxis().apply {
            position = XAxis.XAxisPosition.BOTTOM
            axisMinimum = 0f
            granularity = 1f
            axisMaximum = Matchday.MAX_MATCHDAYS.toFloat()
            textColor = black_n_light_COLOR
            textSize = 13f
            setLabelCount(8, true)
        }

        val data = CombinedData()
        data.setData(generateLineData(thisSeason))
        data.setData(generateBarData(thisSeason))

        val isEmpty = data.barData.entryCount == 0 && data.lineData.entryCount == 0
        if (isEmpty) {
            binding.lableNoData.visibility = View.VISIBLE
            chart.getXAxis().textColor = black_n_light_disbaled_COLOR
            chart.getXAxis().gridColor = black_n_light_disbaled_COLOR
            chart.getXAxis().axisLineColor = black_n_light_disbaled_COLOR
            chart.getAxisLeft().textColor = black_n_light_disbaled_COLOR
        } else {
            binding.lableNoData.visibility = View.INVISIBLE
        }

        chart.data = data
        chart.invalidate()
    }

    private fun generateLineData(thisSeason: Season): LineData {
        val lineData = LineData()
        val entries = mutableListOf<Entry>()
        var pointsCumulative = 0
        val relevantMatchdays = thisSeason.getMatchdaysWithBets()
        relevantMatchdays.forEachIndexed { index, matchday: Matchday? ->
            if (matchday != null) {
                val pointsOfMatchday = matchday.getBetPoints()
                pointsCumulative += pointsOfMatchday
                val entry = Entry(index.toFloat() + 0.8f, pointsCumulative.toFloat())
                entries.add(entry)
            }
        }

        val set = LineDataSet(entries, "lines").apply {
            mode = LineDataSet.Mode.LINEAR
            circleColors = listOf(black_n_light_COLOR)
            circleHoleColor = black_n_light_COLOR
            color = black_n_light_COLOR
            lineWidth = 1.5f
            circleRadius = 3.6f
            circleHoleRadius = 5f
            valueFormatter = LineValueFomatter(pointsCumulative.toFloat())
            valueTextSize = 10f
            valueTextColor = black_n_light_COLOR
        }

        lineData.addDataSet(set)

        return lineData
    }

    private fun generateBarData(thisSeason: Season): BarData {
        val entries = mutableListOf<BarEntry>()
        var pointsCorrectOutcome = 0f
        var pointsCorrectResult = 0f
        thisSeason.getMatchdaysWithBets().forEachIndexed { index, matchday: Matchday? ->
            val pair: Pair<Float, Float> = matchday?.getSplitedBetPoints() ?: Pair(0f, 0f)
            pointsCorrectOutcome += pair.first
            pointsCorrectResult += pair.second
            val entry = BarEntry(
                index.toFloat() + 0.8f,
                floatArrayOf(pointsCorrectOutcome, pointsCorrectResult)
            )
            entries.add(entry)
        }

        val set = BarDataSet(entries, "bars")
        set.highLightAlpha = 100
        set.setDrawValues(false)
        set.setColors(
            context.resources.getColor(R.color.blue),
            context.resources.getColor(R.color.green)
        )

        return BarData(set)
    }

    fun fillPieChart(model: ModelWrapper) {
        val view = binding.seasonPieChart
        val matchesWithBet = model.matchesWithBetAllTime()

        if (matchesWithBet > 0) {
            Matchday.MAX_MATCHDAYS * Matchday.MAX_MATCHES * model.getListOfSeasons().size
            val seasonsDistributionMap = model.getBetDistribution(thisSeason)

            val co = seasonsDistributionMap[BetPoints.RIGHT_OUTCOME] ?: 0 / matchesWithBet
            view.correctOutcomeAmount.text = "($co)"
            val co_p = round2D((co.toDouble() / matchesWithBet.toDouble()) * 100)
            view.correctOutcomePercentage.text = "$co_p%"
            view.chartBlue.progress = co_p.roundToInt()

            val cr = seasonsDistributionMap[BetPoints.RIGHT_RESULT] ?: 0 / matchesWithBet
            view.correctResultAmount.text = "($cr)"
            val cr_p = round2D((cr.toDouble() / matchesWithBet.toDouble()) * 100)
            view.correctResultPercentage.text = "$cr_p%"
            view.chartGreen.progress = cr_p.roundToInt() + co_p.roundToInt()

            val wr = seasonsDistributionMap[BetPoints.WRONG] ?: 0 / matchesWithBet
            view.wrongAmount.text = "($wr)"
            val wr_p = round2D((wr.toDouble() / matchesWithBet.toDouble()) * 100)
            view.wrongPercentage.text = "$wr_p%"
        } else {
            view.apply {
                correctOutcomeAmount.text = "(0)"
                correctOutcomePercentage.text = "0%"
                correctResultAmount.text = "(0)"
                correctResultPercentage.text = "0%"
                wrongAmount.text = "(0)"
                wrongPercentage.text = "0%"
            }
        }
    }

    fun setSeasonHeader(context: Context) {
        val year = thisSeason.getYear()
        binding.seasonHeader.text =
            String.format(context.getString(R.string.seasonString), year, year + 1)
    }

    private fun getRandom(range: Float, start: Float): Float {
        return (Math.random() * range).toFloat() + start
    }

    fun setupPointsCalculation(model: ModelWrapper) {
        val matchesWithBet = model.matchesWithBetAllTime()
        val allSeasonsDistributionMap = model.getAllSeasonsDistributionMap()

        val co = allSeasonsDistributionMap[BetPoints.RIGHT_OUTCOME] ?: 0 / matchesWithBet
        val cr = allSeasonsDistributionMap[BetPoints.RIGHT_RESULT] ?: 0 / matchesWithBet

        val includeLayout = binding.inludePointsCalculation
        // 2 points
        binding.inludePointsCalculation.x2pointsLabel.text = "$co × "
        // 5 points
        binding.inludePointsCalculation.x5pointsLabel.text = " + $cr × "
        // sum
        val sum = co * 2 + cr * 5
        binding.inludePointsCalculation.pointsSumLabel.text =
            String.format(context.getString(R.string.pointsCalculationTemplate), sum)
    }
}

class SeasonStatsAdapter(
    private var listOfSeasons: List<Season>,
    private var model: ModelWrapper,
    private var context: Context
) : RecyclerView.Adapter<SeasonStatsViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeasonStatsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SeasonStatsBinding.inflate(layoutInflater, parent, false)
        return SeasonStatsViewHolder(binding, context)
    }

    override fun getItemCount(): Int {
        return listOfSeasons.size
    }

    override fun onBindViewHolder(holder: SeasonStatsViewHolder, position: Int) {

        holder.setThisSeason(listOfSeasons.reversed()[position])

        holder.setSeasonHeader(context)

        holder.fillPieChart(model)

        holder.buildStatsGraph()

        holder.setupPointsCalculation(model)
    }

}