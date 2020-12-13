package com.jgeig001.kigga.model.repository

import com.jgeig001.kigga.database.LocalDatabase
import com.jgeig001.kigga.model.database.entites.TableElementEntity

class TableElementRepository(val db: LocalDatabase) {

    suspend fun upsertTableElement(tableElementEntity: TableElementEntity) {
        db.gatTableElementDao().upsert(tableElementEntity)
    }

    suspend fun getTablelementsOf(tableID: Int): List<TableElementEntity> {
        return db.gatTableElementDao().getTablelementsOf(tableID)
    }

}