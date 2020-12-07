package com.jgeig001.kigga.model.domain

import java.io.Serializable

/**
 * Spieltag mit mehreren Spielen
 */
data class Matchday(
    var matches: MutableList<Match>,
    var matchdayIndex: Int
) : Serializable, ObservableMatchday() {

    companion object {
        // number of matchdays per season
        @JvmStatic
        val MAX_MATCHDAYS = 34

        // number of matches per matchday
        @JvmStatic
        val MAX_MATCHES = 9
    }

    fun holdsMatch(id: Int): Boolean {
        for (match in this.matches) {
            if (match.matchID == id) {
                return true
            }
        }
        return false
    }

    fun getMatch(id: Int): Match? {
        for (match in this.matches) {
            if (match.matchID == id) {
                return match
            }
        }
        return null
    }

    fun isFinished(): Boolean {
        // finished if last match is finished
        return this.matches.all { it.isFinished() }
    }

    override fun addHomeGoal(match: Match) {
        super.addHomeGoal(match)
        match.addHomeGoal()
    }

    override fun removeHomeGoal(match: Match) {
        super.removeHomeGoal(match)
        match.removeHomeGoal()
    }

    override fun addAwayGoal(match: Match) {
        super.addAwayGoal(match)
        match.addAwayGoal()
    }

    override fun removeAwayGoal(match: Match) {
        super.removeAwayGoal(match)
        match.removeAwayGoal()
    }

    override fun setResult(matchID: Int, matchResult: MatchResult) {
        super.setResult(matchID, matchResult)
        matches.first { it.matchID == matchID }.setResult(matchResult)
    }

    /**
     * returns a list filled with list representing a matchdayDay
     * e.g. [[all matches on friday], [all matches on saturday], [all matches on sunday]]
     * @return ArrayList<ArrayList></ArrayList><Match>>
    </Match> */
    fun matchday_day_iter(): MutableList<MutableList<Match>>? {
        val listOfLists: MutableList<MutableList<Match>> = ArrayList()
        listOfLists.add(mutableListOf())
        var prev: Match = this.matches[0]
        listOfLists[0].add(prev)
        // loop over all matches except the first(:=prev)
        val lis: List<Match> = this.matches.subList(1, this.matches.size)
        for (match in lis) {
            // compare current matchdayDay with prev
            if (match.getMatchdayDate() == prev.getMatchdayDate()) {
                // if they are equal just add
                listOfLists[listOfLists.size - 1].add(match)
            } else {
                // create a new list representing a new matchdayDay
                val newMatchdayDay: ArrayList<Match> = ArrayList()
                newMatchdayDay.add(match)
                listOfLists.add(newMatchdayDay)
            }
            prev = match
        }
        return listOfLists
    }
    /*override fun toString(): String {
        return String.format("%d. Spieltag", this.number);
    }*/

}
