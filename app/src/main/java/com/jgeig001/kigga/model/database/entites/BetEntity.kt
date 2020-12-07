package com.jgeig001.kigga.model.database.entites

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bet_table")
data class BetEntity(
    @PrimaryKey(autoGenerate = true)
    var betID: Int,
    var goals_home: Int,
    var goals_away: Int,
    val matchIDRef: Int
)