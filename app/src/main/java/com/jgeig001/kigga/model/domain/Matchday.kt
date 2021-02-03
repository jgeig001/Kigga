package com.jgeig001.kigga.model.domain

import com.jgeig001.kigga.BR
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
        val match = matches.filter { it.matchID == matchID }.first()
        match.setResult(matchResult)
    }

    fun updateKickoff(matchID: Int, kickoff: Long): Boolean {
        val match = matches.filter { it.matchID == matchID }.first()
        val returnValue = match.updateKickoff(kickoff)
        notifyPropertyChanged(BR.kickoff) // if returnValue==true
        return returnValue
    }

    /**
     * returns a list filled with list representing a matchdayDay
     * e.g. [[all matches on friday], [all matches on saturday], [all matches on sunday]]
     * @return ArrayList<ArrayList></ArrayList><Match>>
    </Match> */
    fun matchday_day_iter(): MutableList<MutableList<Match>> {
        val listOfLists: MutableList<MutableList<Match>> = ArrayList()
        listOfLists.add(mutableListOf())
        val sortedMatches = if (this.kickoffDiff()) {
            matches.sortedBy { match -> match.getKickoff() }
        } else {
            matches
        }
        var prev: Match = sortedMatches[0]
        listOfLists[0].add(prev)
        // loop over all matches except the first(:=prev)
        val lis: List<Match> = sortedMatches.subList(1, sortedMatches.size)
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
        // sort by day
        listOfLists.sortBy { sublis -> sublis.first().getKickoff() }
        return listOfLists
    }

    fun get_DB_ID(): Int {
        return "${matches.first().matchID}${matchdayIndex}".toInt()
    }

    override fun toString(): String {
        val matchesStr = matches.map { m -> "${m.home_team} - ${m.away_team} | " }
        return "Matchday: ($matchdayIndex.) matches:$matchesStr"
    }

    fun getBetPoints(): Int {
        return matches.sumBy { match -> match.getPoints() ?: 0 }
    }

    /**
     * returns a pair of the cumulated points for correct outcome and correct result for this matchday
     */
    fun getSplitedBetPoints(): Pair<Float, Float> {
        var correctOutome = 0
        var correctResult = 0
        for (match in matches) {
            if (match.hasNoBet())
                continue
            when (match.getBetPoints()) {
                BetPoints.RIGHT_OUTCOME -> correctOutome += BetPoints.RIGHT_OUTCOME.points
                BetPoints.RIGHT_RESULT -> correctResult += BetPoints.RIGHT_RESULT.points
                else -> correctResult += 0
            }
        }
        return Pair(correctOutome.toFloat(), correctResult.toFloat())
    }

    fun matchWith(club: Club): Match {
        return matches.filter { m -> m.playedBy(club) }.first()
    }

    /**
     * returns true if all matches do not have same kickoff, else false
     */
    fun kickoffDiff(): Boolean {
        val set = mutableSetOf<Long>()
        for (match in matches) {
            set.add(match.getKickoff())
        }
        return set.size > 1
    }

    /*
    // unused
    fun updateOrder(matchIdList: List<Int>) {
        if (matchIdList.zip(this.matches).all { (shouldID, match) -> shouldID == match.matchID }) {
            // order is equal: sorting not necessary
            return
        }
        // create tmp array
        val matchArray = arrayOfNulls<Match>(matchIdList.size)
        for (i in matchIdList.indices) {
            // sort matches into array
            val searchedMatchID = matchIdList[i]
            val searchedMatch = this.matches.first { match -> match.matchID == searchedMatchID }
            matchArray[i] = searchedMatch
        }
        // convert array to list and assign it as new match list
        this.matches = matchArray.toMutableList() as MutableList<Match>
        notifyChange()
    }
    */

}
