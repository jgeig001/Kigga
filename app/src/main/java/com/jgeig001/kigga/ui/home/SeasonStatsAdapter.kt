package com.jgeig001.kigga.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.jgeig001.kigga.R
import com.jgeig001.kigga.databinding.SeasonStatsBinding
import com.jgeig001.kigga.model.domain.BetPoints
import com.jgeig001.kigga.model.domain.Matchday
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.model.domain.Season
import com.jgeig001.kigga.utils.FloatRounder.round2D
import kotlin.math.roundToInt

// x-Achse = horizontal
// y-Achse = vertical
class SeasonStatsViewHolder(var binding: SeasonStatsBinding, private var context: Context) :
    RecyclerView.ViewHolder(binding.root) {

    private lateinit var thisSeason: Season

    fun setThisSeason(season: Season) {
        thisSeason = season
    }

    fun buildStatsGraph() {

        val chart = binding.statsGraph

        /*
        // working sample
        with(chart) {
            axisLeft.isEnabled = true
            axisRight.isEnabled = false
            xAxis.isEnabled = true
            xAxis.axisMaximum = 110f
            legend.isEnabled = false
            description.isEnabled = false

            // (2)
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
        }

        val entries =
            mutableListOf(
                Entry(1f, 2f),
                Entry(13f, 14f),
                Entry(25f, 26f),
                Entry(29f, 25f),
                Entry(36f, 26f),
                Entry(47f, 29f),
                Entry(49f, 26f),
                Entry(52f, 26f),
                Entry(53f, 55f),
                Entry(55f, 77f),
                Entry(67f, 1f),
                Entry(71f, 12f),
                Entry(85f, 126f)
            )

        val set = LineDataSet(entries, "Line DataSet")
        val lineData = LineData()
        lineData.addDataSet(set)

        val data = CombinedData()
        data.setData(lineData)

        chart.data = data
        chart.invalidate()*/

        chart.setDrawOrder(arrayOf(CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.LINE))

        chart.getDescription().isEnabled = false

        val yAxis = chart.getAxisLeft()
        yAxis.setDrawGridLines(false)
        yAxis.axisMinimum = 0f // this replaces setStartAtZero(true)

        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false

        val xAxis = chart.getXAxis()
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.axisMinimum = 0f
        xAxis.granularity = 1f
        xAxis.axisMaximum = Matchday.MAX_MATCHDAYS.toFloat()

        val data = CombinedData()
        data.setData(generateLineData(thisSeason))
        data.setData(generateBarData(thisSeason))

        chart.setData(data)
        chart.invalidate()

    }

    private fun generateLineData(thisSeason: Season): LineData {
        val lineData = LineData()
        val entries = mutableListOf<Entry>()
        var points = 0
        thisSeason.getMatchdaysWithBets().forEachIndexed { index, matchday: Matchday? ->
            if (matchday != null) {
                points += matchday.getBetPoints()
                val entry = Entry(index.toFloat(), points.toFloat())
                entries.add(entry)
            }
        }

        val set = LineDataSet(entries, "lines")
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.color = context.resources.getColor(R.color.black_n_light)
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
                index.toFloat(),
                floatArrayOf(pointsCorrectOutcome, pointsCorrectResult)
            )
            entries.add(entry)
        }

        val set = BarDataSet(entries, "bars")
        set.highLightAlpha = 100
        set.setColors(
            context.resources.getColor(R.color.blue),
            context.resources.getColor(R.color.green)
        )

        return BarData(set)
    }

    fun fillPieChart(model: ModelWrapper) {
        val view = binding.seasonPieChart
        val matchesWithBet = model.matchesWithBetAllTime()
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
    }

    fun setSeasonHeader(context: Context) {
        val year = thisSeason.getYear()
        binding.seasonHeader.text =
            String.format(context.getString(R.string.seasonString), year, year + 1)
    }

    private fun getRandom(range: Float, start: Float): Float {
        return (Math.random() * range).toFloat() + start
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
    }

}