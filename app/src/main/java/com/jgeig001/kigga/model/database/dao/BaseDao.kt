package com.jgeig001.kigga.model.database.dao

import androidx.room.*

/**
 * generic basic operations
 */
@Dao
interface BaseDao<T> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vararg obj: T)

    @Delete
    suspend fun delete(vararg obj: T)
}

