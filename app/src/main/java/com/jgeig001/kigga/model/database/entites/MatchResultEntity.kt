package com.jgeig001.kigga.model.database.entites

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matchResult_table")
data class MatchResultEntity(
    @PrimaryKey(autoGenerate = true)
    val matchResultID: Int,
    var home_halftime: Int,
    var away_halftime: Int,
    var home_fulltime: Int,
    var away_fulltime: Int,
    var isFinished: Boolean,
    val matchIDRef: Int
)

