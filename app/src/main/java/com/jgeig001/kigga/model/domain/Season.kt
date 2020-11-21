package com.jgeig001.kigga.model.domain

import androidx.databinding.BaseObservable
import java.io.Serializable

class Season(private var matchdays: List<Matchday>, private val year: Int) : Serializable,
    BaseObservable() {

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

    override fun toString(): String {
        return String.format("Saison %d/%d", this.year, this.year + 1)
    }

    fun getMatchday(i: Int): Matchday {
        return matchdays[i]
    }

    fun getCurrentMatchday(): Matchday? {
        for (matchday in matchdays) {
            if (matchday.matches.any { m -> !m.isFinished() }) {
                return matchday
            }
        }
        return null
    }

}
