package com.jgeig001.kigga.model.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.jgeig001.kigga.model.database.entites.ClubEntity

@Dao
interface ClubDao : BaseDao<ClubEntity> {

    @Transaction
    @Query("SELECT * FROM club_table WHERE clubName = :name")
    suspend fun getClubByName(name: String): ClubEntity

    @Transaction
    @Query("SELECT * FROM club_table")
    suspend fun getAllClubs(): List<ClubEntity>

}
