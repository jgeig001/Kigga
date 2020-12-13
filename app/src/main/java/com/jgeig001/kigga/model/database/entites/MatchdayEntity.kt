package com.jgeig001.kigga.model.database.entites

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matchday_table")
data class MatchdayEntity(
    @PrimaryKey
    val matchdayID: Int, // year & matchday_index
    val matchdayIndex: Int,
    val seasonIDRef: Int
)
