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
    var goals_home: Int,
    var goals_away: Int,
    var home_halftime: Int,
    var away_halftime: Int,
    var home_fulltime: Int,
    var away_fulltime: Int,
    var isFinished: Boolean,
    val matchdayIDRef: Int
)
