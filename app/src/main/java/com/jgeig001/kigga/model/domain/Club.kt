package com.jgeig001.kigga.model.domain

import java.io.Serializable

class Club(var clubName: String, var shortName: String) : Serializable {

    override fun toString():String {
        return this.clubName;
    }

}
