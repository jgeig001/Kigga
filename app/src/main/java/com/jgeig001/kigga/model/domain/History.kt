package com.jgeig001.kigga.model.domain

import androidx.databinding.BaseObservable
import java.io.Serializable
import javax.inject.Inject

class History @Inject constructor(
    private var listOfSeasons: MutableList<Season>
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

    fun getLatestSeason(): Season? {
        try {
            return this.listOfSeasons[this.listOfSeasons.size - 1]
        } catch (e: ArrayIndexOutOfBoundsException) {
            return null
        }
    }

    fun getSeasonOf(year: Int): Season? {
        for (season in this.listOfSeasons) {
            if (season.getYear() == year) {
                return season
            }
        }
        return null
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
            var prev: Matchday = season.getMatchdays().reversed()[0]
            for (matchday in season.getMatchdays().reversed()) {
                if (matchday.isFinished()) {
                    return Pair(season, prev)
                }
                prev = matchday
            }
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

}