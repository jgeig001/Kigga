package com.jgeig001.kigga.model.domain

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR
import com.jgeig001.kigga.callbackDispatchers.CallbackDispatcher
import java.io.Serializable

class User(
    private var username: String,
    private var favouriteClub: Club,
    private var bets: HashMap<Match, Bet> = HashMap()
) : Serializable, BaseObservable() {

    @Bindable
    fun getUsername(): String {
        return this.username
    }

    fun setUsername(username: String) {
        println("User.setUsername()")
        this.username = username
        notifyPropertyChanged(BR.username)
    }

    fun usernameShouldChange() {
        println("usernameShouldChange")
    }

    @Bindable
    fun getFavouriteClub(): Club {
        return this.favouriteClub
    }

    fun setFavouriteClub(club: Club) {
        this.favouriteClub = club
        notifyPropertyChanged(BR.favouriteClub)
    }

    @Bindable
    fun getBets(): HashMap<Match, Bet> {
        return this.bets
    }

    fun addBet(bet: Bet) {
        this.bets.put(bet.match, bet)
        notifyPropertyChanged(BR.bets)
    }

    fun deleteBetOn(match: Match) {
        this.bets.remove(match)
        notifyPropertyChanged(BR.bets)
    }

    fun addHomeGoal(match: Match) {
        if (!this.bets.containsKey(match)) {
            this.bets.put(match, Bet(match,1,0))
        } else {
            this.bets.get(match)!!.incHomeGoal()
        }
        notifyPropertyChanged(BR.bets)
    }

    fun removeHomeGoal(match: Match) {
        if (!this.bets.containsKey(match)) {
            this.bets.put(match, Bet(match,0,0))
        } else {
            this.bets.get(match)!!.decHomeGoal()
        }
        notifyPropertyChanged(BR.bets)
    }

    fun addAwayGoal(match: Match) {
        if (!this.bets.containsKey(match)) {
            this.bets.put(match, Bet(match,0,1))
        } else {
            this.bets.get(match)!!.incAwayGoal()
        }
        notifyPropertyChanged(BR.bets)
    }

    fun removeAwayGoal(match: Match) {
        if (!this.bets.containsKey(match)) {
            this.bets.put(match, Bet(match,0,0))
        } else {
            this.bets.get(match)!!.decAwayGoal()
        }
        notifyPropertyChanged(BR.bets)
    }

    fun getPointsAllTime(): Int {
        var sum = 0
        for (bet in this.bets.values) {
            sum += bet.points
        }
        return sum
    }

    val favouriteClubName: String?
        get() = favouriteClub.clubName

}
