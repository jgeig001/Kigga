package com.jgeig001.kigga.model.domain

import java.io.Serializable


class MatchResult : Serializable {

    var home_halftime: Int = -1
    var away_halftime: Int = -1
    var home_fulltime: Int = -1
    var away_fulltime: Int = -1
    var firstHalf_home: Int = -1
    var firstHalf_away: Int = -1
    var secondHalf_home: Int = -1
    var secondHalf_away: Int = -1

    val isHomeWin: Boolean
        get() = if (this.home_fulltime > this.away_fulltime) true else false

    val isDraw: Boolean
        get() = if (this.home_fulltime == this.away_fulltime) true else false

    val isAwayWin: Boolean
        get() = if (this.home_fulltime < this.away_fulltime) true else false

    fun isFinished(): Boolean {
        return this.home_fulltime > -1 && this.away_fulltime > -1
    }

    fun setResults(home_half: Int, away_half: Int, home_full: Int, away_full: Int) {
        this.setHalftimeResult(home_half, away_half)
        this.setFulltimeResult(home_full, away_full)
    }

    fun setHalftimeResult(home: Int, away: Int) {
        this.home_halftime = home
        this.away_halftime = away
    }

    fun setFulltimeResult(home: Int, away: Int) {
        this.home_fulltime = home
        this.away_fulltime = away
    }

    fun setResultFirstHalf(home: Int, away: Int) {
        this.firstHalf_home = home
        this.firstHalf_away = away
    }

    fun setResultSecondHalf(home: Int, away: Int) {
        this.secondHalf_home = home
        this.secondHalf_away = away
    }

    fun getRepr(): String {
        if (this.isFinished()) {
            // Spiel ist fertig
            return String.format(
                "%s:%s(%s:%s)",
                if (this.home_fulltime >= 0) this.home_fulltime.toString() else "-",
                if (this.away_fulltime >= 0) this.away_fulltime.toString() else "-",
                if (this.home_halftime >= 0) this.home_halftime.toString() else "-",
                if (this.away_halftime >= 0) this.away_halftime.toString() else "-"
            )
        } else {
            // NOT finished
            if (this.home_halftime == -1 && this.away_halftime == -1 && this.home_fulltime == -1 && this.away_fulltime == -1) {
                // not started
                return "-:-(-:-)"
            } else if (this.firstHalf_home != -1 && this.firstHalf_away != -1) {
                // zweite Halbzeit läuft
                return String.format(
                    "%s:%d(%d:%d)",
                    this.secondHalf_home,
                    this.secondHalf_away,
                    this.home_halftime,
                    this.away_halftime
                )
            }
            // erste Halbzeit läuft
            return String.format(
                "%d:%d(-:-)",
                this.firstHalf_home,
                this.firstHalf_away
            )
        }
    }

    override fun toString(): String {
        return this.getRepr()
    }

}
