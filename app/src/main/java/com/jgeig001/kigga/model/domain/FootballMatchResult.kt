package com.jgeig001.kigga.model.domain

import java.io.Serializable


class FootballMatchResult(
    private var home_halftime: Int = GOALS_NULL,
    private var away_halftime: Int = GOALS_NULL,
    private var home_fulltime: Int = GOALS_NULL,
    private var away_fulltime: Int = GOALS_NULL,
    private var isFinished: Boolean = false
) : Serializable, MatchResult {

    companion object {
        val GOALS_NULL: Int = -1
    }

    override fun isHomeWin(): Boolean {
        return this.home_fulltime > this.away_fulltime
    }

    override fun isDraw(): Boolean {
        return this.home_fulltime == this.away_fulltime
    }

    override fun isAwayWin(): Boolean {
        return this.home_fulltime < this.away_fulltime
    }

    override fun isFinished(): Boolean {
        return this.isFinished
    }

    override fun finishIt() {
        this.isFinished = true
    }

    override fun resultExists(): Boolean {
        return home_halftime != GOALS_NULL && away_halftime != GOALS_NULL
                && home_fulltime != GOALS_NULL && away_fulltime != GOALS_NULL
    }

    override fun setResults(
        home_halftime: Int,
        away_halftime: Int,
        home_fulltime: Int,
        away_fulltime: Int
    ) {
        this.setHalftimeResult(home_halftime, away_halftime)
        this.setFulltimeResult(home_fulltime, away_fulltime)
    }

    override fun setHalftimeResult(home: Int, away: Int) {
        this.home_halftime = home
        this.away_halftime = away
    }

    override fun setFulltimeResult(home: Int, away: Int) {
        this.home_fulltime = home
        this.away_fulltime = away
    }

    override fun getReprWithHalfTime(): String {
        if (listOf(home_halftime, away_halftime, home_fulltime, away_fulltime)
                .all { it == GOALS_NULL }
        ) {
            // not started
            return "-:-(-:-)"
        }
        return String.format(
            "%s:%s(%s:%s)",
            this.home_fulltime.toString(),
            this.away_fulltime.toString(),
            this.home_halftime.toString(),
            this.away_halftime.toString()
        )
    }

    override fun getReprFullTime(): String {
        return if (this.isFinished) {
            String.format("%s:%s", home_fulltime, away_halftime)
        } else {
            "-:-"
        }
    }

    override fun toString(): String {
        return this.getReprWithHalfTime()
    }

    override fun getHalftimeAway(): Int {
        return away_halftime
    }

    override fun getHalftimeHome(): Int {
        return home_halftime
    }

    override fun getFulltimeAway(): Int {
        return away_fulltime
    }

    override fun getFulltimeHome(): Int {
        return home_fulltime
    }

}
