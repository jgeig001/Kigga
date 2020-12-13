package com.jgeig001.kigga.model.database.entites

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tableElement_table")
data class TableElementEntity(
    @PrimaryKey
    var tableEleID: Int, // year & rank
    var clubName: String,
    var rank: Int,
    var points: Int,
    var goals: Int,
    var opponentGoals: Int,
    var won: Int,
    var draw: Int,
    var lost: Int,
    var matches: Int,
    val tableIDRef: Int
)