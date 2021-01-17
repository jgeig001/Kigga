package com.jgeig001.kigga.model.domain

import androidx.databinding.BaseObservable
import java.io.Serializable

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
        val lis = matchdays.map { md -> if (md.matches.any { m -> m.hasBet() }) md else null }
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

}
