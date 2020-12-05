package com.jgeig001.kigga.model.domain

import android.util.Log
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR
import java.io.Serializable
import javax.inject.Inject

class User @Inject constructor(
    private var favouriteClub: Club?
) : Serializable, BaseObservable() {

    fun getFavouriteClub(): Club? {
        return this.favouriteClub
    }

    fun setFavouriteClub(club: Club?) {
        this.favouriteClub = club
        Log.d("123", "User.setter($club)")
        Log.d("123", this.toString())
    }

    fun hasNoFavouriteClub(): Boolean {
        return favouriteClub == null
    }

    val favouriteClubName: String
        get() = favouriteClub?.clubName ?: ""

}
