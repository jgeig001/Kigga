package com.jgeig001.kigga.model.database.entites.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.jgeig001.kigga.model.database.entites.TableElementEntity
import com.jgeig001.kigga.model.database.entites.TableEntity

data class TableWithTableElements(
    @Embedded val table: TableEntity,
    @Relation(
        parentColumn = "seasonID",
        entityColumn = "tableIDRef"
    )
    val tableElements: List<TableElementEntity>
)
