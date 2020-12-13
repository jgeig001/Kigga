package com.jgeig001.kigga.model.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.jgeig001.kigga.model.database.entites.SeasonEntity

@Dao
interface SeasonDao : BaseDao<SeasonEntity> {

    @Transaction
    @Query("SELECT * FROM season_table")
    suspend fun getAllSeasons(): List<SeasonEntity>

    /**
     * returns list of seasons in ascending order
     * [season2020, season2021, season2022,...]
     */
    @Transaction
    @Query("SELECT * FROM season_table ORDER BY year ASC")
    suspend fun getAllSeasonsASC(): List<SeasonEntity>

    /**
     * returns the nth season
     */
    @Transaction
    @Query("SELECT * FROM (SELECT * FROM season_table ORDER BY year ASC LIMIT :n) AS tbl ORDER BY year DESC LIMIT 1")
    suspend fun get_nth_season(n: Int): SeasonEntity

    /**
     * get the current running season
     */
    @Transaction
    @Query("SELECT * FROM season_table ORDER BY year DESC LIMIT 1")
    suspend fun getLatestSeason(): SeasonEntity

    /**
     * returns the season of [year]
     */
    @Transaction
    @Query("SELECT * FROM season_table WHERE year = :year")
    suspend fun getSeasonOf(year: Int): List<SeasonEntity>

}
