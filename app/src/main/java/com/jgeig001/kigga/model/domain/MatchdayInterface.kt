package com.jgeig001.kigga.model.domain

import android.util.Log
import androidx.databinding.BaseObservable
import com.jgeig001.kigga.BR

interface MatchdayInterface {

    fun addHomeGoal(match: Match)

    fun removeHomeGoal(match: Match)

    fun addAwayGoal(match: Match)

    fun removeAwayGoal(match: Match)

    fun setResult(matchResult: MatchResult)

}

abstract class ObservableMatchday: MatchdayInterface, BaseObservable() {

    override fun addHomeGoal(match: Match) {
        Log.d("123", "ObservableMatchday.addHomeGoal()")
        notifyPropertyChanged(BR.bet)
    }

    override fun removeHomeGoal(match: Match) {
        Log.d("123", "ObservableMatchday.removeHomeGoal()")
        notifyPropertyChanged(BR.bet)
    }

    override fun addAwayGoal(match: Match) {
        Log.d("123", "ObservableMatchday.addAwayGoal()")
        notifyPropertyChanged(BR.bet)
    }

    override fun removeAwayGoal(match: Match) {
        Log.d("123", "ObservableMatchday.removeAwayGoal()")
        notifyPropertyChanged(BR.bet)
    }

    override fun setResult(matchResult: MatchResult) {
        notifyPropertyChanged(BR.matchResult)
    }

}