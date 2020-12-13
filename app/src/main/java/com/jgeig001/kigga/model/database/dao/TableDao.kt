package com.jgeig001.kigga.model.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.jgeig001.kigga.model.database.entites.TableEntity

@Dao
interface TableDao : BaseDao<TableEntity> {

    @Query("SELECT * FROM table_table")
    fun getTableLiveData(): LiveData<TableEntity>

    @Transaction
    @Query("SELECT rank FROM tableElement_table WHERE tableIDRef = :seasonID AND clubName = :clubName")
    suspend fun getRankOf(seasonID: Int, clubName: String): Int

}
