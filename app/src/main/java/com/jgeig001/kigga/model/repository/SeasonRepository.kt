package com.jgeig001.kigga.model.repository

import com.jgeig001.kigga.database.LocalDatabase
import com.jgeig001.kigga.model.database.entites.SeasonEntity

class SeasonRepository(val db: LocalDatabase) {

    suspend fun upsertSeason(seasonEntity: SeasonEntity) {
        db.getSeasonDao().upsert(seasonEntity)
    }

    suspend fun getAllSeasons(): List<SeasonEntity> {
        return db.getSeasonDao().getAllSeasons()
    }

    /**
     * returns list of seasons in ascending order
     * [season2020, season2021, season2022,...]
     */
    suspend fun getAllSeasonsASC(): List<SeasonEntity> {
        return db.getSeasonDao().getAllSeasonsASC()
    }

    /**
     * returns the nth season
     */
    suspend fun get_nth_season(n: Int): SeasonEntity {
        return db.getSeasonDao().get_nth_season(n)
    }

    /**
     * get the current running season
     */
    suspend fun getLatestSeason(): SeasonEntity {
        return db.getSeasonDao().getLatestSeason()
    }

    /**
     * returns the season of [year]
     */
    suspend fun getSeasonOf(year: Int): List<SeasonEntity> {
        return db.getSeasonDao().getSeasonOf(year)
    }

}