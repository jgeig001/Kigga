package com.jgeig001.kigga.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jgeig001.kigga.model.database.entites.*

@Database(
    entities = [
        BetEntity::class,
        ClubEntity::class,
        TableEntity::class,
        TableElementEntity::class,
        MatchResultEntity::class,
        MatchEntity::class,
        MatchdayEntity::class,
        SeasonEntity::class
    ],
    version = 1
)
abstract class MyDatabase : RoomDatabase() {

    companion object {
        // TODO: do this with di hilt
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: MyDatabase? = null

        fun getDatabase(context: Context): MyDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MyDatabase::class.java,
                    "word_database"
                ).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }


}