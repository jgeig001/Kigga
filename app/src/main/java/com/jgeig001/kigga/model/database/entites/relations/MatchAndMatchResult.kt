package com.jgeig001.kigga.model.database.entites.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.jgeig001.kigga.model.database.entites.MatchEntity
import com.jgeig001.kigga.model.database.entites.MatchResultEntity

data class MatchAndMatchResult(
    @Embedded val match: MatchEntity,
    @Relation(
        parentColumn = "matchID",
        entityColumn = "matchIDRef"
    )
    val matchResult: MatchResultEntity
)