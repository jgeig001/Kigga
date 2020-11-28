package com.jgeig001.kigga.model.domain

import com.jgeig001.kigga.utils.HashtagMap
import com.jgeig001.kigga.utils.NameChanger
import java.io.Serializable

class Club(var clubName: String, var shortName: String) : Serializable {

    init {
        if (NameChanger.needChange(clubName)) {
            this.clubName = NameChanger.doChange(clubName)
        }
        if (NameChanger.needChange(shortName)) {
            this.shortName = NameChanger.doChange(shortName)
        }
    }

    var twitterHashtag: String = HashtagMap.hashtagMap[shortName] ?: ""

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
