package com.jgeig001.kigga.ui.table

import android.content.Context
import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.jgeig001.kigga.model.domain.History
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.model.domain.Season
import com.jgeig001.kigga.model.domain.Table
import com.jgeig001.kigga.utils.SharedPreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext

class TableViewModel @ViewModelInject constructor(
    private var model: ModelWrapper,
    @ApplicationContext var context: Context,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {


    var tableLiveData: MutableLiveData<Table>

    init {
        Log.d("1234", "TableViewModel.init{}")
        val selectedSeasonIndex = SharedPreferencesManager.getInt(
            context,
            History.SELECTED_SEASON_SP_KEY
        )
        val season: Season =
            model.getHistory().get_nth_season(selectedSeasonIndex) ?: model.getLatestSeason()
        tableLiveData = MutableLiveData(season.getTable())
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("1234", "TableViewModel.onCleared()!!!")
    }

    // observe SharedPreferences: (IF CHANGE) => set new table

}

// https://dev.to/doodg/sharedprefrences-becomes-live-with-live-data-545g