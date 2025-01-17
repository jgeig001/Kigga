package com.jgeig001.kigga.model.domain

import androidx.databinding.BaseObservable
import java.io.Serializable

/**
 * holds all seasons with all matchdays with all matches
 */
class History (
    private var listOfSeasons: MutableList<Season> = mutableListOf()
) : Serializable, BaseObservable() {

    companion object {
        val SELECTED_SEASON_SP_KEY = "SELECTED_SEASON_SP_KEY"
    }

    fun getListOfSeasons(): MutableList<Season> {
        return this.listOfSeasons
    }

    fun addSeason(season: Season) {
        this.listOfSeasons.add(season)
    }

    fun get_nth_season(n: Int): Season? {
        try {
            return this.listOfSeasons[n]
        } catch (e: IndexOutOfBoundsException) {
            return null
        }
    }

    fun getLatestSeason(): Season {
        return this.listOfSeasons.last()
    }

    fun getMatchdayOf(year: Int, matchdayNumber: Int): Matchday? {
        return this.getSeasonOf(year)?.getMatchdayAtNumber(matchdayNumber)
    }

    fun getSeasonOf(year: Int): Season? {
        return try {
            listOfSeasons.first { it.getYear() == year }
        } catch (e: NoSuchElementException) {
            null
        }
    }

    /**
     * retruns the first season with the first matchday with any results
     * if there are no seasons: return null
     */
    fun getFirstMatchdayWithResults(): Pair<Season, Matchday>? {
        for (season in this.listOfSeasons.reversed()) {
            for (matchday in season.getMatchdays().reversed()) {
                for (match in matchday.matches) {
                    if (match.isFinished()) {
                        return Pair(season, matchday)
                    }
                }
            }
        }
        return null
    }

    fun getFirstMatchdayWithMissingResults(): Pair<Season, Matchday>? {
        for (season in this.listOfSeasons.reversed()) {
            var prev: Matchday = season.getMatchdays().last()
            for (matchday in season.getMatchdays().reversed()) {
                if (matchday.isDone()) {
                    return Pair(season, prev)
                }
                prev = matchday
            }
        }
        if (listOfSeasons.size != 0) {
            // return first matchday of first season
            val s = listOfSeasons.first()
            val md = s.getMatchdays().first()
            return Pair(s, md)
        }
        return null
    }

    fun getMatch(id: Int): Match? {
        for (season in this.listOfSeasons) {
            for (matchday in season.getMatchdays()) {
                if (matchday.holdsMatch(id)) {
                    return matchday.getMatch(id)!!
                }
            }
        }
        return null
    }

    fun getRunningSeason(): Season? {
        listOfSeasons.reversed().forEachIndexed { index, season ->
            if (!season.isFinished()) {
                return get_nth_season(index - 1) ?: season
            }
        }
        return null
    }

    fun getPrevRunningSeason(): Season? {
        val runningSeason = getRunningSeason()
        runningSeason?.let { season ->
            return getPrevSeasonOf(season)
        } ?: return null
    }

    fun getPrevSeasonOf(season: Season): Season? {
        return getSeasonOf(season.getYear() - 1)
    }

    fun getCurrentMatchday(selectedSeason: Int): Matchday? {
        return this.get_nth_season(selectedSeason)?.getCurrentMatchday()
    }

    fun getUnfinishedSeasons(): List<Season> {
        return listOfSeasons.filter { season -> !season.isFinished() }
    }

    fun getFinishedSeasons(): List<Season> {
        return listOfSeasons.filter { season -> season.isFinished() }
    }

    /**
     * returns all seasons since [year]
     */
    fun getSeasonsSince(year: Int): List<Season> {
        return listOfSeasons.filter { it.getYear() >= year }
    }

    fun setListOfSeasons(listOfSeasons: MutableList<Season>) {
        this.listOfSeasons = listOfSeasons
    }

}