package com.jgeig001.kigga.model.domain

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.jgeig001.kigga.R
import java.io.Serializable

/**
 * represents the points the user can get when he/she places a bet
 */
enum class BetPoints(val points: Int, val drawableId: Int) {
    RIGHT_RESULT(5, R.drawable.ic_points_5_dark),
    RIGHT_OUTCOME(2, R.drawable.ic_points_2_dark),
    WRONG(0, R.drawable.ic_points_0_dark);

    companion object {
        fun getDrawable(points: Int): Int {
            return when (points) {
                RIGHT_RESULT.points -> RIGHT_RESULT.drawableId
                RIGHT_OUTCOME.points -> RIGHT_OUTCOME.drawableId
                WRONG.points -> WRONG.drawableId
                else -> 0
            }
        }
    }
}

class Bet(val match: Match, private var goals_home: Int, private var goals_away: Int) :
    Serializable, BaseObservable() {

    var set: Boolean = false

    companion object {
        val NULL_REPR = "x : x"
    }

    @Bindable
    private var bet: IntArray = intArrayOf(goals_home, goals_away)

    val points: Int
        get() {
            if (this.bet[0] == this.match.getMatchResult().home_fulltime && this.bet[1] == this.match.getMatchResult().away_fulltime) {
                return BetPoints.RIGHT_RESULT.points
            }
            if (bet[0] > bet[1] && match.getMatchResult().isHomeWin) {
                return BetPoints.RIGHT_OUTCOME.points
            }
            if (bet[0] == bet[1] && match.getMatchResult().isDraw) {
                return BetPoints.RIGHT_OUTCOME.points
            }
            return if (bet[0] < bet[1] && match.getMatchResult().isAwayWin) {
                BetPoints.RIGHT_OUTCOME.points
            } else BetPoints.WRONG.points
        }

    fun getHomeGoalsStr(): String {
        if (!set)
            return "-"
        return goals_home.toString()
    }

    fun getAwayGoalsStr(): String {
        if (!set)
            return "-"
        return goals_away.toString()
    }

    fun getDrawableId(): Int {
        return BetPoints.getDrawable(this.points)
    }

    fun incHomeGoal() {
        set = true
        this.goals_home += 1
    }

    fun decHomeGoal() {
        if (this.goals_home > 0) {
            set = true
            this.goals_home -= 1
        }
    }

    fun incAwayGoal() {
        set = true
        this.goals_away += 1
    }

    fun decAwayGoal() {
        if (this.goals_away > 0) {
            set = true
            this.goals_away -= 1
        }
    }

    fun repr(): String {
        return if (set) String.format("%d:%d", this.goals_home, this.goals_away) else NULL_REPR
    }

}
