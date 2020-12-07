package com.jgeig001.kigga.model.database.entites

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "table_table")
data class TableEntity(
    @PrimaryKey(autoGenerate = true)
    var tableID: Int,
    var seasonID: Int
)
