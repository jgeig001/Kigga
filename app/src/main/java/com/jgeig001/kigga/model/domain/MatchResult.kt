package com.jgeig001.kigga.model.domain

interface MatchResult {

    fun isHomeWin(): Boolean

    fun isDraw(): Boolean

    fun isAwayWin(): Boolean

    fun isFinished(): Boolean

    fun finishIt()

    fun resultExists(): Boolean

    fun setResults(home_halftime: Int, away_halftime: Int, home_fulltime: Int, away_fulltime: Int)

    fun setHalftimeResult(home: Int, away: Int)

    fun setFulltimeResult(home: Int, away: Int)

    fun getReprWithHalfTime(): String

    fun getReprFullTime(): String

    fun getHalftimeAway(): Int

    fun getHalftimeHome(): Int

    fun getFulltimeAway(): Int

    fun getFulltimeHome(): Int

}