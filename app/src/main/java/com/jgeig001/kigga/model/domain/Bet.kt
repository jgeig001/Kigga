package com.jgeig001.kigga.model.domain

import androidx.databinding.BaseObservable
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
        fun getDrawable(points: Int): Int? {
            return when (points) {
                RIGHT_RESULT.points -> RIGHT_RESULT.drawableId
                RIGHT_OUTCOME.points -> RIGHT_OUTCOME.drawableId
                WRONG.points -> WRONG.drawableId
                else -> null
            }
        }
    }
}

class Bet(
    val match: Match,
    private var goals_home: Int = Match.NO_BET,
    private var goals_away: Int = Match.NO_BET
) :
    Serializable, BaseObservable() {

    fun getBetPoints(): BetPoints? {
        if (goals_home == Match.NO_BET && goals_away == Match.NO_BET)
            return null
        if (goals_home == this.match.fulltimeHome() && goals_away == this.match.fulltimeAway()) {
            return BetPoints.RIGHT_RESULT
        }
        if (goals_home > goals_away && match.getMatchResult().isHomeWin) {
            return BetPoints.RIGHT_OUTCOME
        }
        if (goals_home == goals_away && match.getMatchResult().isDraw) {
            return BetPoints.RIGHT_OUTCOME
        }
        return if (goals_home < goals_away && match.getMatchResult().isAwayWin) {
            BetPoints.RIGHT_OUTCOME
        } else BetPoints.WRONG
    }

    val points: Int
        get() {
            getBetPoints()?.let { return it.points } ?: return -1
        }


    fun betCorrectResult(): Boolean {
        return points == BetPoints.RIGHT_RESULT.points
    }

    fun betCorrectOutcome(): Boolean {
        return points == BetPoints.RIGHT_OUTCOME.points
    }

    fun betWrong(): Boolean {
        val p = points
        return p == BetPoints.WRONG.points || p == -1 // wrong or no bet
    }

    fun getHomeGoalsStr(): String {
        if (goals_home == Match.NO_BET)
            return "-"
        return goals_home.toString()
    }

    fun getAwayGoalsStr(): String {
        if (goals_away == Match.NO_BET)
            return "-"
        return goals_away.toString()
    }

    fun getDrawableResId(): Int? {
        val p = this.points
        return BetPoints.getDrawable(p)
    }

    /* inc & dec */

    fun incHomeGoal() {
        activateBet()
        this.goals_home += 1
    }

    fun decHomeGoal() {
        if (deactivateBet())
            return
        if (this.goals_home > 0) {
            activateBet()
            this.goals_home -= 1
        }
        zerozero()
    }

    fun incAwayGoal() {
        activateBet()
        this.goals_away += 1
    }

    fun decAwayGoal() {
        if (deactivateBet())
            return
        if (this.goals_away > 0) {
            activateBet()
            this.goals_away -= 1
        }
        zerozero()
    }

    private fun zerozero() {
        if (goals_home == Match.NO_BET && goals_away == Match.NO_BET) {
            activateBet()
        }
    }

    fun setHomeGoals(goals: Int) {
        this.goals_home = goals
    }

    fun setAwayGoals(goals: Int) {
        this.goals_away = goals
    }

    private fun activateBet() {
        if (goals_home == Match.NO_BET)
            goals_home = 0
        if (goals_away == Match.NO_BET)
            goals_away = 0
    }

    private fun deactivateBet(): Boolean {
        if (goals_home == 0 && goals_away == 0) {
            goals_home = Match.NO_BET
            goals_away = Match.NO_BET
            return true
        }
        return false
    }

    fun isAvtive(): Boolean {
        return goals_home != Match.NO_BET && goals_away != Match.NO_BET
    }

    fun repr(): String {
        return if (goals_home == Match.NO_BET && goals_away == Match.NO_BET)
            String.format(
                "%d:%d",
                this.goals_home,
                this.goals_away
            )
        else
            Match.NULL_REPR
    }

    fun getHomeGoals(): Int {
        return goals_home
    }

    fun getAwayGoals(): Int {
        return goals_away
    }

}
