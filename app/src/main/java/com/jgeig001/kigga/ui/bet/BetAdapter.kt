package com.jgeig001.kigga.ui.bet

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jgeig001.kigga.R
import com.jgeig001.kigga.databinding.MatchdayCardBinding
import com.jgeig001.kigga.model.domain.Match
import com.jgeig001.kigga.model.domain.Matchday
import java.io.Serializable

class MatchdayViewHolder(private var binding: MatchdayCardBinding) :
    RecyclerView.ViewHolder(binding.root) {
    var matchday_num: TextView = itemView.findViewById(R.id.matchday_x)

    fun bind(matchday_count: Int) {
        binding.matchdayX.text = matchday_count.toString() // duplicate
    }
}

/**
 * adapter for recyclerview in the bet menu
 */
class BetAdapter(
    private var matchdayList: MutableList<Matchday>?,
    private var context: Context
) : RecyclerView.Adapter<MatchdayViewHolder>(), Serializable {

    private var parent: ViewGroup? = null

    // callback
    fun refreshData(matchday: Matchday) {
        Log.d("123", "BetAdapter.refeshData(matchday)")
        matchdayList?.set(matchday.matchdayIndex, matchday)
        for (m in matchday.matches) {
            Log.d("123", m.betRepr())
        }
        notifyDataSetChanged()
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

        val curMatchday = matchdayList?.get(position) ?: return

        val iter: MutableList<MutableList<Match>>? =
            matchdayList?.get(position)?.matchday_day_iter()
        holder.matchday_num.text = String.format("%d. Spieltag", position + 1)

        // iterate of single matchday_day
        if (iter == null)
            return
        for (matchdayDay in iter) {

            // create view for matchdayDay
            val matchday_Day_View = LayoutInflater.from(parent!!.context)
                .inflate(R.layout.view_matchday_day, parent, false)
            (matchday_Day_View.findViewById<View>(R.id.matchday_day_name) as TextView).text =
                matchdayDay[0].getMatchdayDay()

            // loop over all matches of e.g. saturday
            for (match in matchdayDay) {

                // create view element for a match
                if (match.hasStarted()) {
                    val matchView = LayoutInflater.from(parent!!.context)
                        .inflate(R.layout.view_match, parent, false)
                    // and add data to it
                    (matchView.findViewById<View>(R.id.home_team) as TextView).text =
                        match.home_team.shortName
                    (matchView.findViewById<View>(R.id.away_team) as TextView).text =
                        match.away_team.shortName
                    (matchView.findViewById<View>(R.id.result) as TextView).text =
                        match.getMatchResult().getRepr()
                    if (match.isFinished()) {
                        matchView.findViewById<ImageView>(R.id.earned_points).visibility =
                            View.VISIBLE
                        matchView.findViewById<View>(R.id.earned_points).background =
                            ContextCompat.getDrawable(context, match.getDrawableId())
                    } else {
                        matchView.findViewById<ImageView>(R.id.earned_points).visibility =
                            View.INVISIBLE
                    }

                    // add matchView matchday_Day_View
                    (matchday_Day_View as LinearLayout).addView(matchView)

                } else {
                    val matchView = LayoutInflater.from(parent!!.context)
                        .inflate(R.layout.view_match_bet, parent, false)
                    (matchView.findViewById<View>(R.id.home_team) as TextView).text =
                        match.home_team.shortName
                    (matchView.findViewById<View>(R.id.away_team) as TextView).text =
                        match.away_team.shortName
                    val home_plus = matchView.findViewById<ImageButton>(R.id.btn_plus_home)
                    val home_minus = matchView.findViewById<ImageButton>(R.id.btn_minus_home)
                    val away_plus = matchView.findViewById<ImageButton>(R.id.btn_plus_away)
                    val away_minus = matchView.findViewById<ImageButton>(R.id.btn_minus_away)
                    home_plus.setOnClickListener { curMatchday.addHomeGoal(match) }
                    home_minus.setOnClickListener { curMatchday.removeHomeGoal(match) }
                    away_plus.setOnClickListener { curMatchday.addAwayGoal(match) }
                    away_minus.setOnClickListener { curMatchday.removeAwayGoal(match) }
                    // tipp textfield: set text
                    (matchView.findViewById<View>(R.id.goals_bet_home) as TextView).text =
                        match.betHomeGoals()
                    (matchView.findViewById<View>(R.id.goals_bet_away) as TextView).text =
                        match.betAwayGoals()

                    // add matchView matchday_Day_View
                    (matchday_Day_View as LinearLayout).addView(matchView)

                }

            }

            // add matchday_Day_View to matchday_day_layout_holder
            (holder.itemView.findViewById<View>(R.id.matchday_day_layout_holder) as LinearLayout).addView(
                matchday_Day_View
            )
        }
    }

    override fun getItemCount(): Int {
        return this.matchdayList?.size ?: 0
    }
}

// try to do some binding for less coding...
/**
 * EXPERIMENTAL !!!
 */
/*
val inflater = LayoutInflater.from(matchday_Day_View.context)

val binding: ViewMatchBinding = DataBindingUtil.inflate(
    inflater,
    R.layout.view_match,
    matchday_Day_View.findViewById(R.id.matchday_day_layout),
    true
)
binding.setVariable(BR.lololo, "hahaha")*/
