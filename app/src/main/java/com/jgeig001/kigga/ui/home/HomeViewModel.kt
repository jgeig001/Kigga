package com.jgeig001.kigga.ui.home

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.jgeig001.kigga.model.domain.*
import com.jgeig001.kigga.ui.PropertyAwareMutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext

class HomeViewModel @ViewModelInject constructor(
    private var model: ModelWrapper,
    @ApplicationContext private var context: Context,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    var userLiveData: MutableLiveData<User> = MutableLiveData(model.getUser())

    var miniTableLiveDataObjects: MutableList<MutableLiveData<RankedTableElement>> = mutableListOf()

    var points_curSeason: MutableLiveData<String>
    var points_allSeasons: MutableLiveData<String>

    init {
        try {
            points_curSeason = MutableLiveData(model.getPointsCurSeason().toString())
            points_allSeasons = MutableLiveData(model.getPointsAllTime().toString())
        } catch (ex: Exception) {
            points_curSeason = MutableLiveData("")
            points_allSeasons = MutableLiveData("")
        }
        this.fillMiniTable()
    }

    fun fillMiniTable() {
        for (rowData in calc3Table()) {
            miniTableLiveDataObjects.add(MutableLiveData(rowData))
        }
    }

    fun updateMiniTable() {
        calc3Table().forEachIndexed { index, rowData ->
            miniTableLiveDataObjects[index].postValue(rowData)
        }
    }

    private fun calc3Table(): List<RankedTableElement> {
        val table = model.getLatestSeason().getTable()
        // if no fav club: show TOP3
        val favClub: Club = userLiveData.value?.getFavouriteClub() ?: table.getTeam(0).club
        val teams = when {
            table.isLeader(favClub) -> {
                table.getTop3()
            }
            table.isLast(favClub) -> {
                table.getFlop3()
            }
            else -> {
                table.getClubsAround(favClub)
            }
        }
        // transform Pairs to RankedTableElements
        return teams.map { pair -> RankedTableElement(pair.first, pair.second) }
    }

}

/**
 * represents a row of the mini-table
 */
data class RankedTableElement(
    private val tableElement: TableElement,
    private val rank: Int
) {
    fun getRankString(): String {
        return "$rank."
    }

    fun getClubNameShort(): String {
        return tableElement.club.shortName
    }

    fun getPointsString(): String {
        return tableElement.points.toString()
    }
}