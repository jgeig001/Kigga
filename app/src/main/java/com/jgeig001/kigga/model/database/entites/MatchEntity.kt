package com.jgeig001.kigga.model.database.entites

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_table")
data class MatchEntity(
    @PrimaryKey(autoGenerate = false)
    val matchID: Int,
    val match_index: Int, // order number of match in matchday
    val homeTeamName: String,
    val awayTeamName: String,
    val kickoff: Long,
    val rescheduled: Int,
    var home_halftime: Int,
    var away_halftime: Int,
    var home_fulltime: Int,
    var away_fulltime: Int,
    var isFinished: Boolean,
    var bet_home_goals: Int?,
    var bet_away_goals: Int?,
    val matchdayIDRef: Int
)
