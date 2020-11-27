package com.jgeig001.kigga.model.domain

import android.util.Log
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.jgeig001.kigga.R
import java.io.Serializable

/**
 * represents the points the user can get when he/she places a bet
 */
enum class BetPoints(val points: Int, val drawableId: Int) {
    RIGHT_RESULT(5, R.drawable.ic_points_5),
    RIGHT_OUTCOME(2, R.drawable.ic_points_2),
    WRONG(0, R.drawable.ic_points_0);

    companion object {
        fun getDrawable(points: Int): Int {
            return when (points) {
                RIGHT_RESULT.points -> RIGHT_RESULT.drawableId
                RIGHT_OUTCOME.points -> RIGHT_OUTCOME.drawableId
                WRONG.points -> WRONG.drawableId
                else -> WRONG.drawableId
            }
        }
    }
}

class Bet(
    val match: Match,
    private var goals_home: Int = NO_BET,
    private var goals_away: Int = NO_BET
) :
    Serializable, BaseObservable() {

    companion object {
        const val NULL_REPR = "[-:-]"
        const val NO_BET = -1
    }

    @Bindable
    private var bet: IntArray = intArrayOf(goals_home, goals_away)

    val points: Int
        get() {
            if (goals_home == NO_BET && goals_away == NO_BET)
                return -1
            if (goals_home == this.match.fulltimeHome() && goals_away == this.match.fulltimeAway()) {
                return BetPoints.RIGHT_RESULT.points
            }
            if (goals_home > goals_away && match.getMatchResult().isHomeWin) {
                return BetPoints.RIGHT_OUTCOME.points
            }
            if (goals_home == goals_away && match.getMatchResult().isDraw) {
                return BetPoints.RIGHT_OUTCOME.points
            }
            return if (goals_home < goals_away && match.getMatchResult().isAwayWin) {
                BetPoints.RIGHT_OUTCOME.points
            } else BetPoints.WRONG.points
        }

    fun getHomeGoalsStr(): String {
        Log.d(
            "123",
            "##########$match ::: ${goals_home}, ${goals_away} --- ${goals_home.toString()}"
        )
        if (goals_home == NO_BET)
            return "-"
        return goals_home.toString()
    }

    fun getAwayGoalsStr(): String {
        if (goals_away == NO_BET)
            return "-"
        return goals_away.toString()
    }

    fun getDrawableResId(): Int {
        val p = this.points
        return BetPoints.getDrawable(p)
    }

    fun incHomeGoal() {
        activateBet()
        this.goals_home += 1
    }

    fun decHomeGoal() {
        if (this.goals_home > 0) {
            activateBet()
            this.goals_home -= 1
        }
    }

    fun incAwayGoal() {
        activateBet()
        this.goals_away += 1
    }

    fun decAwayGoal() {
        if (this.goals_away > 0) {
            activateBet()
            this.goals_away -= 1
        }
    }

    fun activateBet() {
        if (goals_home == NO_BET)
            goals_home = 0
        if (goals_away == NO_BET)
            goals_away = 0
    }

    fun repr(): String {
        return if (goals_home == NO_BET && goals_away == NO_BET)
            String.format(
                "%d:%d",
                this.goals_home,
                this.goals_away
            )
        else
            NULL_REPR
    }

}
