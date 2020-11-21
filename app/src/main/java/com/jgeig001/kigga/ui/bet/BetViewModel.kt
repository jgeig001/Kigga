package com.jgeig001.kigga.ui.bet

import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.jgeig001.kigga.model.domain.*
import com.jgeig001.kigga.ui.PropertyAwareMutableLiveData

class BetViewModel @ViewModelInject constructor(
    private val model: ModelWrapper,
    private var selectedSeasonIndex: Int,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    var liveDataList: List<PropertyAwareMutableLiveData<Matchday?>>

    init {
        Log.d("123", "history in BetViewModel: ${model.getHistory().hashCode()}")
        this.liveDataList = generateLiveDataList()
    }

    private fun generateLiveDataList(): List<PropertyAwareMutableLiveData<Matchday?>> {
        // create one LiveData object for each matchday
        val lis = mutableListOf<PropertyAwareMutableLiveData<Matchday?>>()
        val season: List<Matchday?> =
            model.getHistory().get_nth_season(selectedSeasonIndex)?.getMatchdays()
                ?: List(Matchday.MAX_MATCHDAYS) { null }
        Log.d("123", "BetViewModel.generateLiveDataList().season: $season")
        for (md: Matchday? in season) {
            val liveDataObject = PropertyAwareMutableLiveData<Matchday?>(md)
            lis.add(liveDataObject)
        }
        return lis.toList()
    }

    fun updateLiveDataList(index: Int) {
        val season = model.getHistory().get_nth_season(index)?.getMatchdays() ?: listOf()
        season.forEachIndexed { i, md ->
            liveDataList[i].postValue(md)
        }
    }

    /**
     * return a list with [Matchday.MAX_MATCHDAYS] elements
     * each representing one matchday
     * if data is not available the will be null as placeholder
     */
    fun getMatchdayList(): MutableList<Matchday?> {
        val lis = MutableList<Matchday?>(Matchday.MAX_MATCHDAYS) { null }
        liveDataList.forEachIndexed { i, ld ->
            ld.value?.let { it -> lis.set(i, it) }
        }
        return lis
    }

    fun setSelectedSeasonIndex(index: Int) {
        this.selectedSeasonIndex = index
        this.updateLiveDataList(index)
    }

    fun getSelectedSeasonIndex(): Int {
        return this.selectedSeasonIndex
    }

    fun getMatchday(i: Int): Matchday? {
        return model.getCurSeason()?.getMatchday(i)
    }

}