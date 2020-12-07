package com.jgeig001.kigga.model.database.entites.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.jgeig001.kigga.model.database.entites.BetEntity
import com.jgeig001.kigga.model.database.entites.MatchEntity

data class MatchAndBet(
    @Embedded val match: MatchEntity,
    @Relation(
        parentColumn = "matchID",
        entityColumn = "matchIDRef"
    )
    val bet: BetEntity
)