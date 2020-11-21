package com.jgeig001.kigga.model.domain

import androidx.databinding.BaseObservable
import java.io.Serializable
import javax.inject.Inject

class ModelWrapper @Inject constructor(
    private var user: User,
    private var liga: Liga,
    private var history: History
) : Serializable, BaseObservable(), ModelAPI {

    // -------------------------------------------- API --------------------------------------------
    override fun getUsername(): String {
        return this.user.getUsername()
    }

    override fun setUsername(username: String) {
        this.user.setUsername(username)
    }

    override fun getPointsCurSeason(): Int {
        history.getLatestSeason()?.let { curSeason ->
            return getPointsOf(curSeason)
        } ?: return 0
    }

    override fun getPointsAllTime(): Int {
        var sum = 0
        for (season in history.getListOfSeasons()) {
            sum += this.getPointsOf(season)
        }
        return sum
    }

    private fun getPointsOf(season: Season): Int {
        var sum = 0
        for (match in season.getAllMatches()) {
            sum += match.getBetPoints()
        }
        return sum
    }

    override fun getFavouriteClub(): Club {
        return this.user.getFavouriteClub()
    }

    override fun setFavouriteClub(club: Club) {
        this.user.setFavouriteClub(club)
    }

    override fun getListOfSeasons(): MutableList<Season> {
        return this.history.getListOfSeasons()
    }

    override fun getCurSeason(): Season? {
        return this.history.getLatestSeason()
    }

    override fun get_nth_season(n: Int): Season? {
        return this.history.get_nth_season(n)
    }

    override fun getBets(): MutableList<Bet> {
        return mutableListOf()
    }

    // ---------------------------------------------------------------------------------------------

    // get components
    fun getUser(): User {
        return this.user
    }

    fun getLiga(): Liga {
        return this.liga
    }

    fun getHistory(): History {
        return this.history;
    }

}

/**
 * The API with all public functions the model offer
 */
interface ModelAPI {

    fun getUsername(): String

    fun setUsername(username: String)

    fun getPointsCurSeason(): Int

    fun getPointsAllTime(): Int

    fun getFavouriteClub(): Club

    fun setFavouriteClub(club: Club)

    fun getListOfSeasons(): MutableList<Season>

    fun getCurSeason(): Season?

    fun get_nth_season(n: Int): Season?

    fun getBets(): MutableList<Bet>

}