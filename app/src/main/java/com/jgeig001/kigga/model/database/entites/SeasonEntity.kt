package com.jgeig001.kigga.model.database.entites

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jgeig001.kigga.model.domain.Matchday

@Entity(tableName = "season_table")
data class SeasonEntity(
    @PrimaryKey(autoGenerate = true)
    val seasonID: Int,
    val year: Int
)
