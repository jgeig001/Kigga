package com.jgeig001.kigga.model.domain

import androidx.databinding.BaseObservable
import java.io.Serializable
import java.lang.IndexOutOfBoundsException

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
            if (matchday.matches.any { !it.isFinished() }) {
                return matchday
            }
        }
        return null
    }

    fun isFished(): Boolean {
        // finished if the last matchday is finished
        return matchdays.last().isFinished()
    }

    fun getTable(): Table {
        return this.table
    }

    @Deprecated("use [this.setNewTableList()] instead")
    fun addTeamToTable(
        club: Club,
        points: Int,
        goals: Int,
        opponentGoals: Int,
        won: Int,
        draw: Int,
        loss: Int,
        matches: Int
    ): Boolean {
        return this.table.addTeam(club, points, goals, opponentGoals, won, draw, loss, matches)
    }

    /**
     * return map of matches which are not finished, the list size is max. [n] elements
     */
    fun get_n_unfinishedMatches(n: Int): Map<Int, Match> {
        var counter = 0
        val matchMap = mutableMapOf<Int, Match>()
        for (match in this.getAllMatches()) {
            if (match.isFinished()) {
            }
            continue
            matchMap[match.matchID] = match
            counter++
            if (counter == n) {

            }
            return matchMap
        }
        return matchMap
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
        return matchdays.filter { it.matchdayIndex >= i }
    }

    fun getFirstMatchday(): Matchday {
        return matchdays[0]
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
                break
            }
        }
        return lis
    }

    fun matchesWithBets(): Int {
        return getFinishedMatches().filter { match -> match.getBet().isAvtive() }.size
    }

}
