package com.jgeig001.kigga.model.domain

import java.io.Serializable


class MatchResult(
    private var home_halftime: Int = GOALS_NULL,
    private var away_halftime: Int = GOALS_NULL,
    private var home_fulltime: Int = GOALS_NULL,
    private var away_fulltime: Int = GOALS_NULL,
    private var isFinished: Boolean = false
) : Serializable {

    // TODO: simplify it
    companion object {
        val GOALS_NULL: Int = -1
    }

    val isHomeWin: Boolean
        get() = this.home_fulltime > this.away_fulltime

    val isDraw: Boolean
        get() = this.home_fulltime == this.away_fulltime

    val isAwayWin: Boolean
        get() = this.home_fulltime < this.away_fulltime

    fun isFinished(): Boolean {
        return this.isFinished
    }

    fun finishIt() {
        this.isFinished = true
    }

    fun setResults(home_halftime: Int, away_halftime: Int, home_fulltime: Int, away_fulltime: Int) {
        this.setHalftimeResult(home_halftime, away_halftime)
        this.setFulltimeResult(home_fulltime, away_fulltime)
    }

    fun setHalftimeResult(home: Int, away: Int) {
        this.home_halftime = home
        this.away_halftime = away
    }

    fun setFulltimeResult(home: Int, away: Int) {
        this.home_fulltime = home
        this.away_fulltime = away
    }

    fun getRepr(): String {
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

    override fun toString(): String {
        return this.getRepr()
    }

    fun getHalftimeAway(): Int {
        return away_halftime
    }

    fun getHalftimeHome(): Int {
        return home_halftime
    }

    fun getFulltimeAway(): Int {
        return away_fulltime
    }

    fun getFulltimeHome(): Int {
        return home_fulltime
    }

}
