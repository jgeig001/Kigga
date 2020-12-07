package com.jgeig001.kigga.model.database.entites

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jgeig001.kigga.model.domain.Club

@Entity(tableName = "match_table")
data class MatchEntity(
    @PrimaryKey(autoGenerate = false)
    val matchID: Int,
    val homeTeamName: String,
    val awayTeamName: String,
    val kickoff: Long,
    val matchdayIDRef: Int
)
