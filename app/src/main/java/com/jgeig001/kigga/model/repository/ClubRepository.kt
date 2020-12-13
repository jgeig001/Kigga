package com.jgeig001.kigga.model.repository

import com.jgeig001.kigga.database.LocalDatabase
import com.jgeig001.kigga.model.database.entites.ClubEntity

class ClubRepository(val db: LocalDatabase) {

    suspend fun upsert(clubEntity: ClubEntity) {
        db.getClubDao().upsert(clubEntity)
    }

    suspend fun getClubByName(name: String): ClubEntity {
        return db.getClubDao().getClubByName(name)
    }

    suspend fun getAllClubs(): List<ClubEntity> {
        return db.getClubDao().getAllClubs()
    }

}