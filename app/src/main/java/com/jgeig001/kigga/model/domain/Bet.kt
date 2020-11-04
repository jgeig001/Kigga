package com.jgeig001.kigga.model.domain

import java.io.Serializable

class Bet(val match: Match, var goals_home: Int, var goals_away: Int) : Serializable {

    val RIGHT_RESULT_POINTS = 5
    val RIGHT_OUTCOME_POINTS = 1
    val WRONG_POINTS = 0

    private var bet: IntArray = intArrayOf(goals_home, goals_away)

    val points: Int
        get() {
            if (this.bet[0] == this.match.getResult().home_fulltime && this.bet[1] == this.match.getResult().away_fulltime) {
                return RIGHT_RESULT_POINTS
            }
            if (bet[0] > bet[1] && match.getResult().isHomeWin) {
                return RIGHT_OUTCOME_POINTS
            }
            if (bet[0] == bet[1] && match.getResult().isDraw) {
                return RIGHT_OUTCOME_POINTS
            }
            return if (bet[0] < bet[1] && match.getResult().isAwayWin) {
                RIGHT_OUTCOME_POINTS
            } else WRONG_POINTS
        }

    fun incHomeGoal() {
        this.goals_home += 1
    }

    fun decHomeGoal() {
        this.goals_home -= 1
        if (this.goals_home < 0) {
            this.goals_home = 0
        }
    }

    fun incAwayGoal() {
        this.goals_away += 1
    }

    fun decAwayGoal() {
        this.goals_away -= 1
        if (this.goals_away < 0) {
            this.goals_away = 0
        }
    }

    fun repr(): String {
        return String.format("%d:%d", this.goals_home, this.goals_away)
    }

}
