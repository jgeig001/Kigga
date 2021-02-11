package com.jgeig001.kigga.model.domain

import androidx.databinding.BaseObservable
import com.jgeig001.kigga.model.exceptions.ClubExistenceException
import java.io.Serializable

// TODO: substitutable with sql db
/**
 * manage all club object
 */
class LigaClass : Serializable, BaseObservable() {

    // key: full clubName, value: club itself
    private var listOfClubs: HashMap<String, Club> = hashMapOf()

    fun getAllClubs(): MutableCollection<Club> {
        return this.listOfClubs.values
    }

    @Throws(ClubExistenceException::class)
    fun getClubBy(name: String): Club {
        val club: Club? = this.listOfClubs[name]
        if (club != null) {
            return club
        }
        throw ClubExistenceException("$name does not exist")
    }

    fun addClub(club: Club) {
        if (!listOfClubs.containsKey(club.clubName))
            this.listOfClubs[club.clubName] = club
    }

    fun removeClub(club: Club) {
        this.listOfClubs.remove(club.clubName)
    }

    fun clubExists(name: String): Boolean {
        return this.listOfClubs.containsKey(name)
    }

    fun anyClubsLoaded() {
        listOfClubs.size > 0
    }

}
