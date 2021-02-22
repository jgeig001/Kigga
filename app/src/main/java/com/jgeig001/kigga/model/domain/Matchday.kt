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

    /**
     * all matches were played and finished
     */
    fun isFinished(): Boolean {
        return this.matches.all { match -> match.isFinished() }
    }

    fun isNotFinished(): Boolean {
        return !isFinished()
    }

    fun isDone(): Boolean {
        return matches.all { match -> match.isFinished() || match.wasSuspended() || match.isRescheduled() }
    }

    fun isNotDone(): Boolean {
        return !isDone()
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

    override fun setResult(matchID: Int, footballMatchResult: FootballMatchResult) {
        super.setResult(matchID, footballMatchResult)
        val match = matches.first { it.matchID == matchID }
        match.setResult(footballMatchResult)
    }

    /**
     * use for exact termination of the match by DFL
     * @returns true if kickoff was changed else false
     */
    fun specifyKickoff(matchID: Int, kickoff: Long): Boolean {
        val match = matches.first { it.matchID == matchID }
        val returnValue = match.specifyKickoff(kickoff)
        notifyPropertyChanged(BR.kickoff) // if returnValue==true
        return returnValue
    }

    fun markAsRescheduled(matchID: Int) {
        val match = matches.first { it.matchID == matchID }
        match.markAsSuspended()
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

    /**
     * generates the PrimaryKey for the database for this matchday
     */
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
        return matches.first { m -> m.playedBy(club) }
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

    fun getMatchdayNumber(): Int {
        return matchdayIndex + 1
    }

    fun earliestKickoff(): Long {
        return matches.minOf { match -> match.getKickoff() }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Matchday)
            return false
        val equalMatches = matches.all { match ->
            other.matches.map { it.matchID }.contains(match.matchID)
        }
        val equalIndex = this.matchdayIndex == other.matchdayIndex
        return equalMatches.and(equalIndex)
    }

}
