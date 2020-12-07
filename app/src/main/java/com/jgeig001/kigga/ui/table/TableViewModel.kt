package com.jgeig001.kigga.ui.table

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.model.domain.Season
import com.jgeig001.kigga.model.domain.Table
import dagger.hilt.android.qualifiers.ApplicationContext

class TableViewModel @ViewModelInject constructor(
    private var model: ModelWrapper,
    private var selectedSeasonIndex: Int,
    @ApplicationContext var context: Context,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    var tableLiveData: MutableLiveData<Table?>

    init {
        tableLiveData = try {
            val season: Season = model.getHistory().get_nth_season(selectedSeasonIndex) ?: model.getLatestSeason()
            MutableLiveData(season.getTable())
        } catch (ex: NoSuchElementException) {
            MutableLiveData(null)
        }
    }


    fun getSelectedSeasonIndex(): Int {
        return this.selectedSeasonIndex
    }

    fun setSelectedSeasonIndex(index: Int) {
        this.selectedSeasonIndex = index
        this.updateLiveDataList(index)
    }

    private fun updateLiveDataList(index: Int) {
        model.getHistory().get_nth_season(index)?.getTable().let { table ->
            tableLiveData.postValue(table)
        }
    }

}

// https://dev.to/doodg/sharedprefrences-becomes-live-with-live-data-545g