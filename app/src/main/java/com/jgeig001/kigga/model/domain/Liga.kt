package com.jgeig001.kigga.model.domain

import androidx.databinding.BaseObservable
import com.jgeig001.kigga.model.exceptions.ClubExistenceException
import java.io.Serializable
import javax.inject.Inject
import kotlin.collections.HashMap

interface Factory<T> {
    fun create(): T
}

// TODO: convert to kotlin object class ?!

class Liga @Inject constructor() : Serializable, BaseObservable() {

    companion object : Factory<Liga> {

        override fun create(): Liga = Liga()

        // key: full clubName, value: club itself
        var listOfClubs: HashMap<String, Club> = hashMapOf()

        @JvmStatic
        fun getAllClubs(): MutableCollection<Club> {
            return this.listOfClubs.values
        }

        @JvmStatic
        @Throws(ClubExistenceException::class)
        fun getClubBy(name: String): Club {
            var club: Club? = this.listOfClubs.get(name)
            if (club != null) {
                return club
            }
            throw ClubExistenceException(name + " does not exist")
        }

        @JvmStatic
        fun addClub(club: Club) {
            this.listOfClubs.put(club.clubName!!, club)
        }

        @JvmStatic
        fun removeClub(club: Club) {
            this.listOfClubs.remove(club.clubName)
        }

        @JvmStatic
        fun clubExists(name: String): Boolean {
            return this.listOfClubs.containsKey(name)
        }

    }
}
