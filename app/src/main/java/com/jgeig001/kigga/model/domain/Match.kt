package com.jgeig001.kigga.model.domain

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class Match(
    val matchID: Int,
    val home_team: Club,
    val away_team: Club,
    @Bindable
    private var kickoff: Long,
    @Bindable
    private var matchResult: MatchResult = FootballMatchResult(),
    private var suspensionState: SuspensionState = SuspensionState.REGULAR
) : MatchResult, Serializable, BaseObservable() {

    companion object {
        const val NULL_REPR = "[-:-]"
        const val NO_BET = -1
    }

    @Bindable
    private var bet: Bet = Bet(this)

    init {
        // try to mark SUSPENDED matches as RESCHEDULED => user can bet again
        if (this.wasSuspended()) {
            if (afterHours24() && !kickoffDone())
                suspensionState = SuspensionState.RESCHEDULED
        }
    }

    private fun afterHours24(): Boolean {
        val now: Long = Calendar.getInstance().time.time
        val hours12: Long = 24 * 3600
        return kickoff + hours12 < now
    }

    fun markAsRescheduled() {
        suspensionState = SuspensionState.RESCHEDULED
    }

    fun markAsSuspended() {
        suspensionState = SuspensionState.SUSPENDED
    }

    fun getSuspendionState(): SuspensionState {
        return suspensionState
    }

    fun isRegular(): Boolean {
        return suspensionState == SuspensionState.REGULAR
    }

    fun regularKickoff(): Boolean {
        return suspensionState == SuspensionState.REGULAR
    }

    fun wasSuspended(): Boolean {
        return suspensionState == SuspensionState.SUSPENDED
    }

    fun isRescheduled(): Boolean {
        return suspensionState == SuspensionState.RESCHEDULED
    }

    fun setResult(footballMatchResult: FootballMatchResult) {
        this.matchResult = footballMatchResult
    }

    fun matchOf(club: Club): Boolean {
        return home_team == club || away_team == club
    }

    fun fulltimeHome(): Int {
        return matchResult.getFulltimeHome()
    }

    fun fulltimeAway(): Int {
        return matchResult.getFulltimeAway()
    }

    fun isNotFinished(): Boolean {
        return !this.matchResult.isFinished()
    }

    fun hasNotStarted(): Boolean {
        return !this.resultExists()
    }

    fun kickoffDone(): Boolean {
        return resultExists()
    }

    fun isLive(): Boolean {
        return kickoffDone() && isNotFinished()
    }

    /**
     * use for exact termination of the match by DFL
     * @returns true if kickoff was changed else false
     */
    fun specifyKickoff(newKickoff: Long): Boolean {
        if (this.kickoff != newKickoff) {
            this.kickoff = newKickoff
            if (wasSuspended())
            // match was suspended and got a new appointment
                suspensionState = SuspensionState.RESCHEDULED
            return true
        }
        return false
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

    fun getBetHomeGoalsStr(): String {
        return bet.getHomeGoalsStr()
    }

    fun getBetAwayGoalsStr(): String {
        return bet.getAwayGoalsStr()
    }

    fun getBetHomeGoals(): Int {
        return bet.getHomeGoals()
    }

    fun getBetAwayGoals(): Int {
        return bet.getAwayGoals()
    }

    fun betRepr(): String {
        return bet.repr()
    }

    fun correctResultBet(): Boolean {
        return bet.betCorrectResult()
    }

    fun correctOutcomeBet(): Boolean {
        return bet.betCorrectOutcome()
    }

    fun wrongBet(): Boolean {
        return bet.betWrong()
    }

    /**
     * returns the points user gets
     * if there was no bet made, it returns
     */
    fun getPoints(): Int? {
        return this.bet.points
    }

    fun getBetPoints(): BetPoints? {
        return this.bet.getBetPoints()
    }

    fun getBetResultDrawableResId(): Int? {
        return bet.getDrawableResId()
    }

    /**
     * initializes [this.bet] and returns it, but because of shitty optionals you can not 100% sure
     */
    private fun getBet(): Bet {
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
            "[%d] %s : %s; [%s] => %s, %d Uhr: %s",
            matchID,
            home_team.shortName,
            away_team.shortName,
            this.getKickoffClock(),
            this.matchResult.getReprWithHalfTime(),
            this.kickoff, this.suspensionState
        )
    }

    fun ligaPointsFor(club: Club): Int? {
        if (!this.matchOf(club))
            return null
        when {
            matchResult.isDraw() -> {
                return 1
            }
            club == home_team -> {
                if (matchResult.isHomeWin()) {
                    return 3
                }
                return 0
            }
            club == away_team -> {
                if (matchResult.isAwayWin()) {
                    return 3
                }
                return 0
            }
            else -> return null
        }
    }

    fun setBet(bet: Bet) {
        this.bet = bet
    }

    fun hasBet(): Boolean {
        return bet.isAvtive()
    }

    fun hasNoBet(): Boolean {
        return !this.hasBet()
    }

    fun playedBy(club: Club): Boolean {
        return club == home_team || club == away_team
    }

    fun getOtherClub(club: Club): Club {
        return if (home_team == club) away_team else home_team
    }

    fun getHomeGoalsStr(): String {
        return getBet().getHomeGoalsStr()
    }

    fun getAwayGoalsStr(): String {
        return getBet().getAwayGoalsStr()
    }

    fun setHomeGoals(goals: Int) {
        getBet().setHomeGoals(goals)
    }

    fun setAwayGoals(goals: Int) {
        getBet().setAwayGoals(goals)
    }

    override fun isHomeWin(): Boolean {
        return matchResult.isHomeWin()
    }

    override fun isDraw(): Boolean {
        return matchResult.isDraw()
    }

    override fun isAwayWin(): Boolean {
        return matchResult.isAwayWin()
    }

    override fun isFinished(): Boolean {
        return this.matchResult.isFinished()
    }

    override fun finishIt() {
        matchResult.finishIt()
    }

    override fun resultExists(): Boolean {
        return matchResult.resultExists()
    }

    override fun setResults(
        home_halftime: Int,
        away_halftime: Int,
        home_fulltime: Int,
        away_fulltime: Int
    ) {
        matchResult.setResults(home_halftime, away_halftime, home_fulltime, away_fulltime)
    }

    override fun setHalftimeResult(home: Int, away: Int) {
        matchResult.setHalftimeResult(home, away)
    }

    override fun setFulltimeResult(home: Int, away: Int) {
        matchResult.setFulltimeResult(home, away)
    }

    override fun getReprWithHalfTime(): String {
        return matchResult.getReprWithHalfTime()
    }

    override fun getReprFullTime(): String {
        return matchResult.getReprFullTime()
    }

    override fun getHalftimeAway(): Int {
        return matchResult.getHalftimeAway()
    }

    override fun getHalftimeHome(): Int {
        return matchResult.getHalftimeHome()
    }

    override fun getFulltimeAway(): Int {
        return matchResult.getFulltimeAway()
    }

    override fun getFulltimeHome(): Int {
        return matchResult.getFulltimeHome()
    }

    fun setMatchResult(matchResult: MatchResult) {
        this.matchResult = matchResult
    }

}
