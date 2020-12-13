package com.jgeig001.kigga.model.database.dao

import androidx.room.*
import com.jgeig001.kigga.model.database.entites.MatchEntity

@Dao
interface MatchDao : BaseDao<MatchEntity> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity)

    @Transaction
    @Query("SELECT * FROM match_table WHERE matchID = :id")
    fun getMatchByID(id: Int): MatchEntity

    @Transaction
    @Query("SELECT * FROM match_table WHERE matchdayIDRef = :matchdayID ORDER BY match_index")
    fun getMatchesOfMatchday(matchdayID: Int): List<MatchEntity>

    @Transaction
    @Query("UPDATE match_table SET bet_home_goals = :bet_home_goals AND bet_away_goals = :bet_away_goals WHERE matchID = :matchID")
    fun updateBets(matchID: Int, bet_home_goals: Int, bet_away_goals: Int)

}
