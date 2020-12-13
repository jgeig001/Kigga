package com.jgeig001.kigga.model.database.entites

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "club_table")
data class ClubEntity(
    @PrimaryKey
    var clubName: String,
    var shortName: String,
    var twitterHashtag: String
)
