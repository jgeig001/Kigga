package com.jgeig001.kigga.model.database.entites

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matchday_table")
data class MatchdayEntity(
    @PrimaryKey(autoGenerate = true)
    val matchdayID: Int,
    val matchdayIndex: Int,
    val seasonIDRef: Int
)
