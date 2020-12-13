package com.jgeig001.kigga.model.database.entites.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.jgeig001.kigga.model.database.entites.MatchEntity
import com.jgeig001.kigga.model.database.entites.MatchdayEntity

data class MatchdayWithMatches(
    @Embedded val matchday: MatchdayEntity,
    @Relation(
        parentColumn = "matchdayID",
        entityColumn = "matchdayIDRef"
    )
    val matches: List<MatchEntity>
)
