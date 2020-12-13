package com.jgeig001.kigga.model.database.entites.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.jgeig001.kigga.model.database.entites.MatchdayEntity
import com.jgeig001.kigga.model.database.entites.SeasonEntity

data class SeasonWithMatchdays(
    @Embedded val season: SeasonEntity,
    @Relation(
        parentColumn = "year",
        entityColumn = "seasonIDRef"
    )
    val matchdays: List<MatchdayEntity>
)
