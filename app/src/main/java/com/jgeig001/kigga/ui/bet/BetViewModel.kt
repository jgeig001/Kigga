package com.jgeig001.kigga.ui.bet

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.jgeig001.kigga.model.domain.Match
import com.jgeig001.kigga.model.domain.Matchday
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.ui.PropertyAwareMutableLiveData

class BetViewModel @ViewModelInject constructor(
    private val model: ModelWrapper,
    private var selectedSeasonIndex: Int,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    var liveDataList: MutableList<PropertyAwareMutableLiveData<Matchday>>

    init {
        this.liveDataList = generateLiveDataList(this.selectedSeasonIndex)
    }

    fun generateLiveDataList(seasonIndex: Int): MutableList<PropertyAwareMutableLiveData<Matchday>> {
        // create one LiveData object for each matchday
        val lis = mutableListOf<PropertyAwareMutableLiveData<Matchday>>()
        val season =
            model.getHistory().get_nth_season(seasonIndex)?.getMatchdays() ?: mutableListOf()
        for (md in season) {
            val liveDataObject = PropertyAwareMutableLiveData<Matchday>()
            liveDataObject.value = md
            lis.add(liveDataObject)
        }
        return lis
    }

    fun getMatchdayList(): MutableList<Matchday> {
        val lis = mutableListOf<Matchday>()
        for (ld in liveDataList) {
            ld.value?.let { lis.add(it) }
        }
        return lis
    }

    fun setSelectedSeasonIndex(index: Int) {
        this.selectedSeasonIndex = index
        liveDataList = this.generateLiveDataList(this.selectedSeasonIndex)
    }

    fun getSelectedSeasonIndex(): Int {
        return this.selectedSeasonIndex
    }

    fun addHomeGoal(match: Match) {
        this.model.addHomeGoal(match)
    }

    fun removeHomeGoal(match: Match) {
        this.model.removeHomeGoal(match)
    }

    fun addAwayGoal(match: Match) {
        this.model.addAwayGoal(match)
    }

    fun removeAwayGoal(match: Match) {
        this.model.removeAwayGoal(match)
    }

    fun getMatchday(i: Int): Matchday? {
        return model.getCurSeason()?.getMatchday(i)
    }

}