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

    private var bet: Bet = Bet(this)

    @Bindable
    fun getMatchResult(): MatchResult {
        return this.matchResult
    }

    fun setResult(matchResult: MatchResult) {
        this.matchResult = matchResult
    }

    fun matchOf(club: Club): Boolean {
        return home_team == club || away_team == club
    }

    fun hasNotStarted(): Boolean {
        return !this.isFinished() && !this.isRunning()
    }

    fun fulltimeHome(): Int {
        return matchResult.getFulltimeHome()
    }

    fun fulltimeAway(): Int {
        return matchResult.getFulltimeAway()
    }

    fun hasStarted(): Boolean {
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("Europe/Berlin")
        val now: Long = cal.time.time
        return now > this.kickoff
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
        val date = Date(this.kickoff)
        return sdf.format(date)
    }

    fun getMatchdayDate(): String {
        val sdf = SimpleDateFormat("dd.MM.")
        val date = Date(this.kickoff)
        return sdf.format(date)
    }

    fun getMatchdayDay(): String {
        val sdf = SimpleDateFormat("EEE")
        val date = Date(this.kickoff)
        return sdf.format(date)
    }

    fun getBetHomeGoals(): String {
        return bet.getHomeGoalsStr()
    }

    fun getBetAwayGoals(): String {
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
    fun getBetPoints(): Int {
        return this.bet?.let { it.points } ?: 0
    }

    fun getBetResultDrawableResId(): Int {
        return bet.getDrawableResId()
    }

    /**
     * initializes [this.bet] and returns it, but beacause of shitty optionals you can not 100% sure
     */
    fun getBet(): Bet {
        return this.bet
    }

    fun addHomeGoal() {
        Log.d("123", "Match.addHomeGoal()")
        this.getBet().incHomeGoal()
    }

    fun removeHomeGoal() {
        Log.d("123", "Match.removeHomeGoal()")
        this.getBet().decHomeGoal()
    }

    fun addAwayGoal() {
        Log.d("123", "Match.addAwayGoal()")
        this.getBet().incAwayGoal()
    }

    fun removeAwayGoal() {
        Log.d("123", "Match.removeAwayGoal()")
        this.getBet().decAwayGoal()
    }

    override fun toString(): String {
        return String.format(
            "[%d] %s : %s; [%s] => %s, %d Uhr",
            matchID,
            home_team.shortName,
            away_team.shortName,
            this.getKickoffClock(),
            this.matchResult.getRepr(),
            this.kickoff
        )
    }

    fun ligaPointsFor(club: Club): Int? {
        if (!this.matchOf(club))
            return null
        if (matchResult.isDraw)
            return 1
        if (club == home_team) {
            if (matchResult.isHomeWin) {
                return 3
            }
            return 0
        } else if (club == away_team) {
            if (matchResult.isAwayWin) {
                return 3
            }
            return 0
        }
        return null
    }


}
