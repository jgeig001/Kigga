package com.jgeig001.kigga.model.domain

import androidx.databinding.BaseObservable
import java.io.Serializable
import java.util.*

data class Season(private var matchdays: List<Matchday>, private val year: Int) : Serializable,
    BaseObservable() {

    private var table: Table = Table()

    fun getMatchdays(): List<Matchday> {
        return this.matchdays
    }

    fun getMatchdayIndexOf(matchday: Matchday): Int {
        return this.matchdays.indexOf(matchday)
    }

    fun getAllMatches(): MutableList<Match> {
        val allMatches = mutableListOf<Match>()
        for (matchday in this.matchdays) {
            for (match in matchday.matches) {
                allMatches.add(match)
            }
        }
        return allMatches
    }

    fun getYear(): Int {
        return this.year
    }

    fun getMatchday(i: Int): Matchday {
        return matchdays[i]
    }

    fun getCurrentMatchday(): Matchday? {
        for (matchday in matchdays) {
            if (matchday.isNotDone()) {
                return matchday
            }
        }
        return null
    }

    fun isFinished(): Boolean {
        return matchdays.all { matchday -> matchday.isFinished() }
    }

    fun getTable(): Table {
        return this.table
    }

    /**
     * returns the matchday of matchday [matchdayNumber]
     * parameter 1 will return the first matchday of the season
     * if the param is out of bound it will return null
     */
    fun getMatchdayAtNumber(matchdayNumber: Int): Matchday? {
        return try {
            matchdays[matchdayNumber - 1]
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }

    /**
     * returns a list with all matchdays since index [i] (inclusive)
     */
    fun getMatchdaysSinceIndex(i: Int): List<Matchday> {
        return matchdays.subList(i, matchdays.size)
    }

    fun getFirstMatchday(): Matchday {
        return matchdays[0]
    }

    /**
     * returns all suspended and rescheduled matches in the past as map
     * {matchday_i: [match0, match1,...],...}
     */
    fun getOldSuspendedMatches(): Map<Matchday, List<Match>> {
        val returnMap = mutableMapOf<Matchday, MutableList<Match>>()
        val now: Long = Calendar.getInstance().time.time
        for (matchday in matchdays) {
            for (match in matchday.matches) {
                if (match.getKickoff() > now)
                    return returnMap
                if (match.wasSuspended() || match.isRescheduled()) {
                    if (returnMap.containsKey(matchday)) {
                        // create new sublist and add match
                        val lis: MutableList<Match>? = returnMap[matchday]
                        lis?.add(match) ?: run {
                            returnMap[matchday] = mutableListOf(match)
                        }
                    } else {
                        // just add match to sublist
                        returnMap[matchday] = mutableListOf(match)
                    }
                }
            }
        }
        return returnMap
    }

    fun setTableList(lis: MutableList<TableElement>) {
        getTable().setNewTableList(lis)
    }

    override fun toString(): String {
        return "Saison ${this.year}/${this.year + 1}"
    }

    fun getFinishedMatches(): List<Match> {
        val lis = mutableListOf<Match>()
        for (match in getAllMatches()) {
            if (match.isFinished()) {
                lis.add(match)
            } else {
                if (match.isRegular())
                    break
            }
        }
        return lis
    }

    fun matchesWithBetsSize(): Int {
        return getFinishedMatches().filter { match -> match.hasBet() }.size
    }

    /**
     * list with an element for each matchday with any bets
     * if on a matchday any bet was made it holds the matchday
     * if no bet was made it contains null as placeholder
     * the last n matchdays with no bets are dropped
     * e.g. [md, md, null, md, null, md, md, md]
     */
    fun getMatchdaysWithBets(): List<Matchday?> {
        val lis =
            matchdays.map { matchday ->
                if (matchday.matches.any { match ->
                        match.hasBet() && match.isFinished()
                    })
                    matchday else null
            }
        // delete all matchdays with no bets at the list end
        var delCounter = 0
        for (md in lis.reversed()) {
            if (md == null)
                delCounter += 1
            else
                break
        }
        return lis.dropLast(delCounter)
    }

    fun getMatchesOfClub(club: Club): List<Match> {
        return matchdays.map { md -> md.matchWith(club) }
    }

    /**
     * try to find matches which seems to be RESCHEDULED and mark them as it
     */
    fun findRescheduledMatches() {
        for (i in 0 until Matchday.MAX_MATCHDAYS) {
            val curMatchday = matchdays[i]
            if (curMatchday.isFinished())
                continue // because finished matchday has no suspended matches
            for (j in 0 until Matchday.MAX_MATCHES) {
                val curMatch = curMatchday.matches[j]
                if (curMatch.isFinished())
                    continue // because finished matches can not be suspended
                val afterNextMatchday = try {
                    val nextMatchday = matchdays[i + 1]
                    val nextMatchdayStart = nextMatchday.earliestKickoff()
                    nextMatchdayStart < curMatch.getKickoff()
                } catch (ex: IndexOutOfBoundsException) {
                    // suspended matches of last matchday do not matter
                    null
                }
                if (afterNextMatchday == true) {
                    curMatch.markAsRescheduled()
                }
            }
        }
    }

    /**
     * try to mark SUSPENDED matches as RESCHEDULED
     */
    fun checkSuspendedMatches() {
        matchdays.forEachIndexed { index, matchday ->
            for (match in matchday.matches) {
                if (match.wasSuspended()) {
                    // if the kickoff (eventually updated/rescheduled) lays behind the next matchday
                    // ...the kickoff have to be rescheduled
                    val nextMatchdayStart = try {
                        matchdays[index + 1]
                    } catch (e: IndexOutOfBoundsException) {
                        matchdays.last()
                    }.matches.first().getKickoff()
                    if (match.getKickoff() > nextMatchdayStart) {
                        match.markAsRescheduled()
                    }
                }
            }
        }
    }

}
