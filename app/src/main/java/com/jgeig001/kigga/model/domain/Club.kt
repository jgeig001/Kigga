package com.jgeig001.kigga.model.domain

import com.jgeig001.kigga.utils.HashtagMap
import java.io.Serializable

class Club(var clubName: String, var shortName: String, var twitterHashtag: String = "") : Serializable {

    init {
        twitterHashtag = HashtagMap.hashtagMap[shortName] ?: ""
    }

    fun setHastagAgain() {
        twitterHashtag = HashtagMap.hashtagMap[shortName] ?: ""
    }

    override fun toString(): String {
        return this.clubName;
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Club) {
            return false
        }
        val c: Club = other
        return this.clubName == c.clubName && this.shortName == c.shortName
    }

}
