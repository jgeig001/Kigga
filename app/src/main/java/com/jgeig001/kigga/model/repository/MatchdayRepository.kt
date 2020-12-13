package com.jgeig001.kigga.model.repository

import androidx.lifecycle.LiveData
import com.jgeig001.kigga.database.LocalDatabase
import com.jgeig001.kigga.model.database.entites.MatchdayEntity
import com.jgeig001.kigga.model.database.entites.TableEntity

class MatchdayRepository(val db: LocalDatabase) {

    suspend fun upsertMatchday(matchdayEntity: MatchdayEntity) {
        db.getMatchdayDao().upsert(matchdayEntity)
    }

    fun getMatchdayLiveDataOf(index: Int): LiveData<MatchdayEntity> {
        return db.getMatchdayDao().getMatchdayLiveDataOf(index)
    }

    suspend fun getAllMatchdaysOfSeason(seasonID: Int): List<MatchdayEntity> {
        return db.getMatchdayDao().getAllMatchdaysOfSeason(seasonID)
    }

}