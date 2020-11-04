package com.jgeig001.kigga.model.domain

import androidx.databinding.BaseObservable
import androidx.databinding.Observable
import com.jgeig001.kigga.callbackDispatchers.CallbackDispatcher
import com.jgeig001.kigga.callbackDispatchers.ObservableModel
import java.io.Serializable
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ModelWrapper(
    private var user: User,
    private var liga: Liga,
    private var history: History
) : Serializable, BaseObservable(), ObservableModel, ModelAPI {

    /* notifyMap holds all callback functions
     * key: the id saved in BR file which refers to one property in the model
     * value: List with all callbackfunctions executed if property changes
     */
    private val notifyMap: HashMap<Int, ArrayList<(sender: Observable?, propertyId: Int) -> Unit>> =
        HashMap()

    private var callbackDispatcher: CallbackDispatcher

    init {
        callbackDispatcher = CallbackDispatcher(this.notifyMap)
        this.user.addOnPropertyChangedCallback(callbackDispatcher)
        this.history.addOnPropertyChangedCallback(callbackDispatcher)
        this.liga.addOnPropertyChangedCallback(callbackDispatcher)
    }

    // --------------------------------------- Notifications ---------------------------------------
    override fun getNotified(
        BR_property_ID: Int,
        callbackFunction: (sender: Observable?, propertyId: Int) -> Unit
    ) {
        print("size:" + this.notifyMap.size + "\n\n")
        for ((k, v) in this.notifyMap) {
            print(k)
            print(" ::: ")
            println(v)
        }
        this.callbackDispatcher.registerCallback(BR_property_ID, callbackFunction)
    }
    // ---------------------------------------------------------------------------------------------


    // -------------------------------------------- API --------------------------------------------
    override fun getUsername(): String {
        return this.user.getUsername()
    }

    override fun setUsername(username: String) {
        println("modelWrapper.setUsername()")
        this.user.setUsername(username)
    }

    override fun getPointsCurSeason(): Int {
        var sum = 0
        var allMatches = ArrayList<Match>()
        if (history.getCurSeason() == null) {
            return 0
        }
        for (matchday in history.getCurSeason()!!.getMatchdays()) {
            for (match in matchday.matches) {
                allMatches.add(match)
            }
        }
        for (bet in user.getBets().values) {
            if (allMatches.contains(bet.match)) {
                sum += bet.points
            }
        }
        return sum
    }

    override fun getPointsAllTime(): Int {
        return this.user.getPointsAllTime()
    }

    override fun getFavouriteClub(): Club {
        return this.user.getFavouriteClub()
    }

    override fun setFavouriteClub(club: Club) {
        this.user.setFavouriteClub(club)
    }

    override fun getListOfSeasons(): ArrayList<Season> {
        return this.history.getListOfSeasons()
    }

    override fun getCurSeason(): Season? {
        return this.history.getCurSeason()
    }

    override fun get_nth_season(n: Int): Season {
        return this.history.get_nth_season(n)
    }

    override fun getBets(): HashMap<Match, Bet> {
        return this.user.getBets()
    }

    override fun addHomeGoal(match: Match) {
        this.user.addHomeGoal(match)
    }

    override fun removeHomeGoal(match: Match) {
        this.user.removeHomeGoal(match)
    }

    override fun addAwayGoal(match: Match) {
        this.user.addAwayGoal(match)
    }

    override fun removeAwayGoal(match: Match) {
        this.user.removeAwayGoal(match)
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

    fun getListOfSeasons(): ArrayList<Season>

    fun getCurSeason(): Season?

    fun get_nth_season(n: Int): Season

    fun getBets(): HashMap<Match, Bet>

    fun addHomeGoal(match: Match)

    fun removeHomeGoal(match: Match)

    fun addAwayGoal(match: Match)

    fun removeAwayGoal(match: Match)
}