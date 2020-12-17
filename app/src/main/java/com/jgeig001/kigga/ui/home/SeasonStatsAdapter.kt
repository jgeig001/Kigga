package com.jgeig001.kigga.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jgeig001.kigga.R
import com.jgeig001.kigga.databinding.SeasonStatsBinding
import com.jgeig001.kigga.model.domain.BetPoints
import com.jgeig001.kigga.model.domain.Matchday
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.model.domain.Season
import com.jgeig001.kigga.utils.FloatRounder.round2D
import kotlin.math.roundToInt

class SeasonStatsViewHolder(var binding: SeasonStatsBinding) :
    RecyclerView.ViewHolder(binding.root) {

    private lateinit var thisSeason: Season

    fun setThisSeason(season: Season) {
        thisSeason = season
    }

    fun buildStatsGraph() {
        val stats_graphView = binding.statsGraph
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

}

class SeasonStatsAdapter(
    private var listOfSeasons: List<Season>,
    private var model: ModelWrapper,
    private var context: Context
) : RecyclerView.Adapter<SeasonStatsViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeasonStatsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SeasonStatsBinding.inflate(layoutInflater, parent, false)
        return SeasonStatsViewHolder(binding)
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