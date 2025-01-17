package com.jgeig001.kigga.model.domain

import androidx.databinding.BaseObservable
import com.jgeig001.kigga.BR

interface MatchdayInterface {

    fun addHomeGoal(match: Match)

    fun removeHomeGoal(match: Match)

    fun addAwayGoal(match: Match)

    fun removeAwayGoal(match: Match)

    fun setResult(matchID: Int, footballMatchResult: FootballMatchResult)

}

abstract class ObservableMatchday : MatchdayInterface, BaseObservable() {

    override fun addHomeGoal(match: Match) {
        notifyPropertyChanged(BR.bet)
    }

    override fun removeHomeGoal(match: Match) {
        notifyPropertyChanged(BR.bet)
    }

    override fun addAwayGoal(match: Match) {
        notifyPropertyChanged(BR.bet)
    }

    override fun removeAwayGoal(match: Match) {
        notifyPropertyChanged(BR.bet)
    }

    override fun setResult(matchID: Int, footballMatchResult: FootballMatchResult) {
        notifyPropertyChanged(BR.matchResult)
    }

}