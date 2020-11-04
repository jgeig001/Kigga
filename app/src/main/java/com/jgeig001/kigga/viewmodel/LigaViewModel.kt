package com.jgeig001.kigga.viewmodel

import androidx.databinding.Observable
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jgeig001.kigga.model.domain.*
import java.io.Serializable

class LigaViewModel(private val model: ModelWrapper, private var selectedSeasonIndex: Int) :
    Serializable, ViewModel() {

    @Transient private  var _matchday_list =
        MutableLiveData(model.get_nth_season(selectedSeasonIndex).getMatchdays())
    @Transient private var _bets = MutableLiveData(model.getBets())

    @Transient val matchday_list: LiveData<ArrayList<Matchday>> = _matchday_list
    @Transient val bets: LiveData<HashMap<Match, Bet>> = _bets

    init {
        // hand over some callback functions to model: so model can notify viewmodel
        this.model.getNotified(BR.result, this::anyMatchdayChangedCallback)
        this.model.getNotified(BR.bets, this::anyBetChangedCallback)
    }

    // ------------------------------------ callback functions -------------------------------------
    private fun anyMatchdayChangedCallback(sender: Observable?, propertyId: Int) {
        this._matchday_list.value =
            this.model.getListOfSeasons()[this.selectedSeasonIndex].getMatchdays()
    }

    private fun anyBetChangedCallback(sender: Observable?, propertyId: Int) {
        this._bets.value = this.model.getBets()
    }
    // ---------------------------------------------------------------------------------------------

    fun getMatchdayList(): java.util.ArrayList<Matchday>? {
        return this._matchday_list.value
    }

    fun setSelectedSeasonIndex(index: Int) {
        this.selectedSeasonIndex = index
        _matchday_list.value = this.model.get_nth_season(this.selectedSeasonIndex).getMatchdays()
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

}