package com.jgeig001.kigga.model.domain

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class Match(
    val matchID: Int,
    val home_team: Club,
    val away_team: Club,
    private val kickoff: Long,
    private var result: Result
) : Serializable, BaseObservable() {

    @Bindable
    fun getResult(): Result {
        return this.result
    }

    fun setResult(result: Result) {
        this.result = result
        notifyPropertyChanged(BR.result)
    }

    fun isFinished(): Boolean {
        if (!this.result.isFinished()) {
            return false
        }
        return true
    }

    fun isRunning(): Boolean {
        val now:Long = Calendar.getInstance().getTime().time
        return if (now > this.kickoff) true else false
    }

    fun getKickoff(): Long {
        return this.kickoff
    }

    fun getKickoffClock(): String {
        val sdf = SimpleDateFormat("HH:mm")
        val date = java.util.Date(this.kickoff)
        return sdf.format(date)
    }

    fun getMatchdayDate(): String {
        val sdf = SimpleDateFormat("dd.MM.")
        val date = java.util.Date(this.kickoff)
        return sdf.format(date)
    }

    fun getMatchdayDay(): String {
        val sdf = SimpleDateFormat("EEE")
        val date = java.util.Date(this.kickoff)
        return sdf.format(date)
    }

    override fun toString(): String {
        return String.format(
            "[%d] %s : %s; [%s] => %s",
            matchID,
            home_team.shortName,
            away_team.shortName,
            this.getKickoffClock(),
            this.result.getRepr()
        )
    }
}
