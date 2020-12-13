package com.jgeig001.kigga.model.repository

import com.jgeig001.kigga.database.LocalDatabase
import com.jgeig001.kigga.model.database.entites.TableEntity

class TableRepository(val db: LocalDatabase) {

    suspend fun upsertTable(tableEntity: TableEntity) {
        db.getTableDao().upsert(tableEntity)
    }

    suspend fun getRankOf(seasonID: Int, clubName: String): Int {
        return db.getTableDao().getRankOf(seasonID, clubName)
    }

}