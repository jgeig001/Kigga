package com.jgeig001.kigga.model.repository

import com.jgeig001.kigga.database.LocalDatabase
import com.jgeig001.kigga.model.database.entites.MatchEntity

class MatchRepository(val db: LocalDatabase) {

    suspend fun upsertMatch(matchEntity: MatchEntity) {
        db.getMatchDao().upsert(matchEntity)
    }

    fun getMatchWIthID(id: Int): MatchEntity {
        return db.getMatchDao().getMatchByID(id)
    }

    fun getMatchesOfMatchday(matchdayID: Int): List<MatchEntity> {
        return db.getMatchDao().getMatchesOfMatchday(matchdayID)
    }

    suspend fun updateBets(matchID: Int, bet_home_goals: Int, bet_away_goals: Int) {
        db.getMatchDao().updateBets(matchID, bet_home_goals, bet_away_goals)
    }


}