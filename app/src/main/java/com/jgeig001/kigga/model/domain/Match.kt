package com.jgeig001.kigga.model.domain

import android.util.Log
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class Match(
    val matchID: Int,
    val home_team: Club,
    val away_team: Club,
    private val kickoff: Long,
    private var matchResult: MatchResult
) : Serializable, BaseObservable() {

    private var bet: Bet

    init {
        bet = Bet(this, 0, 0)
    }

    @Bindable
    fun getMatchResult(): MatchResult {
        return this.matchResult
    }

    fun setResult(matchResult: MatchResult) {
        this.matchResult = matchResult
    }

    fun hasNotStarted(): Boolean {
        return !this.isFinished() && !this.isRunning()
    }

    fun hasStarted(): Boolean {
        val now: Long = Calendar.getInstance().getTime().time
        return if (now > this.kickoff) true else false
    }

    fun isFinished(): Boolean {
        return this.matchResult.isFinished()
    }

    fun isRunning(): Boolean {
        return this.hasStarted() && !this.isFinished()
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

    fun betHomeGoals(): String {
        return bet.getHomeGoalsStr()
    }

    fun betAwayGoals(): String {
        return bet.getAwayGoalsStr()
    }

    fun betRepr(): String {
        Log.d("123", "Match.betRepr()")
        return bet.repr()
    }

    /**
     * returns the points user gets
     * if there was no bet made, it returns 0
     */
    fun getPoints(): Int {
        return this.bet?.let { it.points } ?: 0
    }

    fun getDrawableId(): Int {
        return bet.getDrawableId()
    }

    /**
     * initializes [this.bet] and returns it, but beacause of shitty optionals you can not 100% sure
     */
    fun getBet(): Bet {
        return this.bet
    }

    fun addHomeGoal() {
        this.getBet().incHomeGoal()
    }

    fun removeHomeGoal() {
        this.getBet().decHomeGoal()
    }

    fun addAwayGoal() {
        this.getBet().incAwayGoal()
    }

    fun removeAwayGoal() {
        this.getBet().decAwayGoal()
    }

    override fun toString(): String {
        return String.format(
            "[%d] %s : %s; [%s] => %s",
            matchID,
            home_team.shortName,
            away_team.shortName,
            this.getKickoffClock(),
            this.matchResult.getRepr()
        )
    }


}
