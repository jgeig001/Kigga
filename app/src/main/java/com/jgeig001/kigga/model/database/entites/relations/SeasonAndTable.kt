package com.jgeig001.kigga.model.database.entites.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.jgeig001.kigga.model.database.entites.SeasonEntity
import com.jgeig001.kigga.model.database.entites.TableEntity

data class SeasonAndTable(
    @Embedded val season: SeasonEntity,
    @Relation(
        parentColumn = "seasonID",
        entityColumn = "seasonID"
    )
    val table: TableEntity
)