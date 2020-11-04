package com.jgeig001.kigga.model.domain

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR
import java.io.Serializable
import java.util.ArrayList

class Season(private var matchdays: ArrayList<Matchday>, private val year: Int) : Serializable,
    BaseObservable() {

    @Bindable
    fun getMatchdays(): ArrayList<Matchday> {
        return this.matchdays
    }

    fun setMatchdays(matchdays: ArrayList<Matchday>) {
        this.matchdays = matchdays
        notifyPropertyChanged(BR.matchdays)
    }

    fun addMatchday(matchday: Matchday) {
        this.matchdays.add(matchday)
        notifyPropertyChanged(BR.matchdays)
    }

    fun getMatchdayIndexOf(matchday: Matchday): Int {
        return this.matchdays.indexOf(matchday)
    }

    fun getAllMatches(): ArrayList<Match> {
        val allMatches = ArrayList<Match>()
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


}
