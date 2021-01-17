package com.jgeig001.kigga.ui.bet

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jgeig001.kigga.R
import com.jgeig001.kigga.databinding.MatchdayCardBinding
import com.jgeig001.kigga.model.domain.Match
import com.jgeig001.kigga.model.domain.Matchday
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.utils.ArrowFunctions
import com.jgeig001.kigga.utils.SeasonSelect
import com.jgeig001.kigga.utils.TrendCalculator
import com.jgeig001.kigga.utils.WeekdayTranslator
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*


class MatchdayViewHolder(var binding: MatchdayCardBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun setMatchdayNumber(i: Int, context: Context) {
        // "%d. Spieltag"
        val s = context.getString(R.string.matchdayString)
        itemView.findViewById<TextView>(R.id.matchday_x).text = String.format(s, i + 1)
    }

    fun thisMatchday() {
        this.binding.matchdayCard.setBackgroundResource(R.drawable.corners_gradient)
    }

}

/**
 * adapter for recyclerview in the bet menu
 */
class BetAdapter(
    private var matchdayList: MutableList<Matchday?>,
    private var model: ModelWrapper,
    private var context: Context
) : RecyclerView.Adapter<MatchdayViewHolder>(), Serializable {

    private var parent: ViewGroup? = null

    // callback
    fun refreshData(matchday: Matchday) {
        matchdayList[matchday.matchdayIndex] = matchday
        notifyDataSetChanged()
    }

    fun afterFirstLoadDone(matchdayList: MutableList<Matchday?>) {
        for (i in 0 until Matchday.MAX_MATCHDAYS) {
            this.matchdayList[i] = matchdayList[i]
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchdayViewHolder {
        this.parent = parent
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MatchdayCardBinding.inflate(layoutInflater, parent, false)
        return MatchdayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MatchdayViewHolder, position: Int) {
        // fix: prevents it from recycling, otherwise: strange things will happen <- dynamic added views are the reason for that(okay, it's maybe a bit ugly)
        holder.itemView.setHasTransientState(true)
        holder.binding.matchdayX.text = context.getString(R.string.matchdayString, position + 1)
        holder.binding.initLoaderProgressBar.visibility = View.VISIBLE
        val thisMatchday = matchdayList[position] ?: return

        // change look
        holder.setMatchdayNumber(position, context)

        // check if this matchday ist the current matchday
        val selectedSeasonIndex = SeasonSelect.getSelectedSeasonIndex(context)
        val curMatchday = model.getCurrentMatchday(selectedSeasonIndex)
        val isCurMatchday: Boolean = curMatchday == thisMatchday
        if (isCurMatchday)
            holder.thisMatchday()

        val iter: MutableList<MutableList<Match>> =
            matchdayList[position]?.matchday_day_iter() ?: return

        val table = model.get_nth_season(selectedSeasonIndex)?.getTable()

        // iterate of single matchday_day
        for (matchdayDay in iter) {

            // create view for matchdayDay
            val matchday_Day_View: LinearLayout = LayoutInflater.from(parent!!.context)
                .inflate(R.layout.view_matchday_day, parent, false) as LinearLayout

            matchday_Day_View.findViewById<TextView>(R.id.matchday_day_name).text =
                context.getString(
                    WeekdayTranslator.matchdayStrings[matchdayDay[0].getMatchdayDay()] ?: 0
                )

            matchday_Day_View.findViewById<TextView>(R.id.matchday_day_date).text =
                SimpleDateFormat("dd.MM.yyyy").format(
                    Date(matchdayDay.first().getKickoff())
                )

            if (!isCurMatchday) {
                matchday_Day_View.background =
                    ContextCompat.getDrawable(context, R.drawable.corners_stroke_other_matchdays)
            } else {
                holder.binding.matchdayX.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.light
                    )
                )
            }
            // setting another background resets the padding
            matchday_Day_View.setPadding(dpToPx(15), 0, dpToPx(15), dpToPx(6))
            matchday_Day_View.elevation = 8f

            // loop over all matches of e.g. saturday
            for (match in matchdayDay) {

                var matchView: LinearLayout
                // create view element for a match
                if (match.isFinished()) {
                    matchView = LayoutInflater.from(parent!!.context)
                        .inflate(R.layout.view_match_finished, parent, false) as LinearLayout
                    // and add data to it
                    matchView.findViewById<TextView>(R.id.home_team).text =
                        match.home_team.shortName
                    matchView.findViewById<TextView>(R.id.away_team).text =
                        match.away_team.shortName

                    matchView.findViewById<TextView>(R.id.result_home).text =
                        match.getMatchResult().getFulltimeHome().toString()
                    matchView.findViewById<TextView>(R.id.result_away).text =
                        match.getMatchResult().getFulltimeAway().toString()

                    val betResultViewHome = matchView.findViewById<TextView>(R.id.done_bet_home)
                    val betResultViewAway = matchView.findViewById<TextView>(R.id.done_bet_away)
                    betResultViewHome.visibility = View.VISIBLE
                    betResultViewHome.text = "[${match.getHomeGoalsStr()}"
                    betResultViewAway.visibility = View.VISIBLE
                    betResultViewAway.text = ":${match.getAwayGoalsStr()}]"

                    // display earned points...
                    val earnedPointsView = matchView.findViewById<ImageView>(R.id.earned_points)
                    if (match.isFinished()) {
                        match.getBetResultDrawableResId()?.let { imageID ->
                            earnedPointsView.visibility = View.VISIBLE
                            earnedPointsView.background =
                                ContextCompat.getDrawable(context, imageID)
                        } ?: run { earnedPointsView.visibility = View.VISIBLE }

                    } else {
                        earnedPointsView.visibility = View.INVISIBLE
                    }

                } else if (match.isRunning()) {
                    // • LIVE
                    matchView = LayoutInflater.from(parent!!.context)
                        .inflate(R.layout.view_match_live, parent, false) as LinearLayout

                    // and add data to it
                    matchView.findViewById<TextView>(R.id.home_team).text =
                        match.home_team.shortName
                    matchView.findViewById<TextView>(R.id.away_team).text =
                        match.away_team.shortName

                    val betResultViewHome = matchView.findViewById<TextView>(R.id.done_bet_home)
                    val betResultViewAway = matchView.findViewById<TextView>(R.id.done_bet_away)
                    betResultViewHome.visibility = View.VISIBLE
                    betResultViewHome.text = "[${match.getHomeGoalsStr()}"
                    betResultViewAway.visibility = View.VISIBLE
                    betResultViewAway.text = ":${match.getAwayGoalsStr()}]"

                    matchView.findViewById<TextView>(R.id.result_layout).text =
                        match.getMatchResult().getReprWithHT()

                } else {

                    /** user can bet on match */

                    matchView = LayoutInflater.from(parent!!.context)
                        .inflate(R.layout.view_match_bet, parent, false) as LinearLayout

                    matchView.gravity = Gravity.CENTER_HORIZONTAL

                    table?.getRankOf(match.home_team)
                    matchView.findViewById<TextView>(R.id.rank_home).text =
                        "${table?.getRankOf(match.home_team)}."
                    matchView.findViewById<TextView>(R.id.rank_away).text =
                        "${table?.getRankOf(match.away_team)}."

                    matchView.findViewById<TextView>(R.id.home_team).text =
                        match.home_team.shortName
                    matchView.findViewById<TextView>(R.id.away_team).text =
                        match.away_team.shortName

                    // calc trend
                    (model.get_nth_season(SeasonSelect.getSelectedSeasonIndex(context))
                        ?: model.getRunningSeason())?.let {
                        val trendHome = TrendCalculator.calcTrend(
                            it,
                            model.getHistory(),
                            match.home_team
                        )
                        matchView.findViewById<TextView>(R.id.trend_home).text =
                            trendHome.toString()
                        matchView.findViewById<ImageView>(R.id.trend_arrow_home).rotation =
                            ArrowFunctions.trendArrow(trendHome)
                        matchView.findViewById<ImageView>(R.id.trend_arrow_home)
                            .setImageDrawable(
                                context.getDrawable(ArrowFunctions.getArrowDrawable(trendHome))
                            )
                        val trendAway = TrendCalculator.calcTrend(
                            it,
                            model.getHistory(),
                            match.away_team
                        )
                        matchView.findViewById<TextView>(R.id.trend_away).text =
                            trendAway.toString()
                        matchView.findViewById<ImageView>(R.id.trend_arrow_away).rotation =
                            ArrowFunctions.trendArrow(trendAway)
                        matchView.findViewById<ImageView>(R.id.trend_arrow_away)
                            .setImageDrawable(
                                context.getDrawable(ArrowFunctions.getArrowDrawable(trendAway))
                            )
                    }


                    matchView.findViewById<TextView>(R.id.kickoff).text =
                        context.getString(
                            R.string.clock, SimpleDateFormat("HH:mm").format(
                                Date(match.getKickoff())
                            )
                        )

                    val home_plus = matchView.findViewById<ImageButton>(R.id.btn_plus_home)
                    val home_minus = matchView.findViewById<ImageButton>(R.id.btn_minus_home)
                    val away_plus = matchView.findViewById<ImageButton>(R.id.btn_plus_away)
                    val away_minus = matchView.findViewById<ImageButton>(R.id.btn_minus_away)
                    home_plus.setOnClickListener { thisMatchday.addHomeGoal(match) }
                    home_minus.setOnClickListener { thisMatchday.removeHomeGoal(match) }
                    away_plus.setOnClickListener { thisMatchday.addAwayGoal(match) }
                    away_minus.setOnClickListener { thisMatchday.removeAwayGoal(match) }
                    // tipp textfield: set text
                    (matchView.findViewById<View>(R.id.goals_bet_home) as TextView).text =
                        match.getBetHomeGoalsStr()
                    (matchView.findViewById<View>(R.id.goals_bet_away) as TextView).text =
                        match.getBetAwayGoalsStr()

                    val homeHashtag = match.home_team.twitterHashtag//.toLowerCase()
                    if (homeHashtag.isBlank())
                        match.home_team.setHastagAgain()
                    val awayHashtag = match.away_team.twitterHashtag//.toLowerCase()
                    if (awayHashtag.isBlank())
                        match.away_team.setHastagAgain()
                    if (homeHashtag != "" || awayHashtag != "") {
                        matchView.findViewById<TextView>(R.id.twitter_hashtag).text =
                            context.getString(
                                R.string.twitter_hashtag_template,
                                homeHashtag,
                                awayHashtag
                            )
                        matchView.findViewById<TextView>(R.id.twitter_link).text =
                            context.getString(
                                R.string.twitter_hashtag_link,
                                match.home_team.twitterHashtag + match.away_team.twitterHashtag
                            )
                    } else {
                        matchView.findViewById<TextView>(R.id.twitter_link).text =
                            context.getString(R.string.twitter_hashtag_link, "BuLi")
                    }

                }

                // add matchView matchday_Day_View
                matchday_Day_View.addView(matchView)

                if (match != matchdayDay.last()) {
                    val divider = LayoutInflater.from(parent!!.context)
                        .inflate(R.layout.simple_divider, parent, false)
                    matchday_Day_View.addView(divider)

                }

            }

            // add matchday_Day_View to matchday_day_layout_holder
            val matchday_day_layout_holder =
                holder.itemView.findViewById<LinearLayout>(R.id.matchday_day_layout_holder)
            matchday_day_layout_holder.addView(matchday_Day_View)
            if (matchdayDay != iter.last()) {
                val space = LayoutInflater.from(parent!!.context)
                    .inflate(R.layout.just_space, parent, false)
                matchday_day_layout_holder.addView(space)
            }

        }
        holder.binding.initLoaderProgressBar.visibility = View.GONE
    }

    override fun getItemCount(): Int {
        return this.matchdayList.size
    }

    fun dpToPx(dp: Int): Int {
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }
}
