package com.jgeig001.kigga.model.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.jgeig001.kigga.model.database.entites.TableElementEntity

@Dao
interface TableElementDao : BaseDao<TableElementEntity> {

    @Transaction
    @Query("SELECT * FROM tableElement_table WHERE tableIDRef = :tableID ORDER BY rank")
    suspend fun getTablelementsOf(tableID: Int): List<TableElementEntity>

}