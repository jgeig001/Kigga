package com.jgeig001.kigga.model.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.jgeig001.kigga.model.database.entites.MatchdayEntity

@Dao
interface MatchdayDao : BaseDao<MatchdayEntity> {

    @Transaction
    @Query("SELECT * FROM matchday_table WHERE matchdayIndex = :index")
    fun getMatchdayLiveDataOf(index: Int): LiveData<MatchdayEntity>

    @Transaction
    @Query("SELECT * FROM matchday_table WHERE seasonIDRef = :seasonID ORDER BY matchdayIndex")
    suspend fun getAllMatchdaysOfSeason(seasonID: Int): List<MatchdayEntity>

}
