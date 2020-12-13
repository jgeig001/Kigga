package com.jgeig001.kigga.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jgeig001.kigga.model.database.dao.*
import com.jgeig001.kigga.model.database.entites.*

@Database(
    entities = [
        ClubEntity::class,
        TableEntity::class,
        TableElementEntity::class,
        MatchEntity::class,
        MatchdayEntity::class,
        SeasonEntity::class
    ],
    version = 1
)
abstract class LocalDatabase : RoomDatabase() {

    abstract fun getSeasonDao(): SeasonDao
    abstract fun getTableDao(): TableDao
    abstract fun gatTableElementDao(): TableElementDao
    abstract fun getMatchDao(): MatchDao
    abstract fun getClubDao(): ClubDao
    abstract fun getMatchdayDao(): MatchdayDao

}