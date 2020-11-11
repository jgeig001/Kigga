package com.jgeig001.kigga.model.domain

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR
import java.io.Serializable
import javax.inject.Inject

class User @Inject constructor(
    private var username: String,
    private var favouriteClub: Club
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

    val favouriteClubName: String?
        get() = favouriteClub.clubName

}
