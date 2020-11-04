package com.jgeig001.kigga.model.domain

import com.jgeig001.kigga.model.exceptions.MatchExsitenceException
import java.io.Serializable
import java.util.ArrayList

/*
* Spieltag mit mehreren Spielen
* */
class Matchday(var matches: ArrayList<Match>) : Serializable {

    companion object {
        // number of matchdays per season
        @JvmStatic
        val MAX_MATCHDAYS = 34
        // number of matches per matchday
        @JvmStatic
        val MAX_MATCHES = 9
    }



    fun holdsMatch(id: Int): Boolean {
        for (match in this.matches) {
            if (match.matchID == id) {
                return true
            }
        }
        return false
    }

    fun getMatch(id: Int): Match? {
        for (match in this.matches) {
            if (match.matchID == id) {
                return match
            }
        }
        return null
    }

    fun isFinished(): Boolean {
        if (this.matches.reversed().get(0).isFinished()) {
            return true
        }
        return false
    }

    /*override fun toString(): String {
        return String.format("%d. Spieltag", this.number);
    }*/

}
