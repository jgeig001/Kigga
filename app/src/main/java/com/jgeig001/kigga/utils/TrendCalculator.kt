package com.jgeig001.kigga.utils

import com.jgeig001.kigga.model.domain.*
import kotlin.math.roundToInt

object TrendCalculator {

    val pointsNormalized = mapOf(0 to 0.0f, 1 to 0.33f, 3 to 1.0f, null to 0f)

    fun calcTrend(model: ModelWrapper, club: Club): Float {
        var sum = 0f
        val last5 = last5(model, club)
        val multipliers = listOf(6f, 4f, 4f, 3f, 3f)
        for ((match, multiplicator) in last5.zip(multipliers)) {
            match.ligaPointsFor(club)?.let { ligaPoints ->
                val pn = pointsNormalized[ligaPoints]
                sum += pn?.times(multiplicator) ?: 0f
            }
        }
        val r = sum / multipliers.sum()
        return ((r * 100.0).roundToInt() / 100.0).toFloat()
    }

    /**
     * Generates a list with the last 5 matches of this [club].
     * The list starts with the latest match and ends with the oldest.
     * e.g. [MD_5, MD,_4, MD_3, MD_2, MD_1]
     * Its also possible that are less than 5 matches available.
     * Than the list still starts with the latest match and ends with the oldest match available.
     * e.g. [MD_3, MD_2, MD_1]
     * If there is no data the list is empty.
     * If the last 5 matches are in two seasons it will give you the last machtes of the prevoius season.
     * e.g. [MD_3, MD_2, MD_1, MD_34, MD_33]
     */
    fun last5(model: ModelWrapper, club: Club): List<Match> {
        val five = 5
        val lis = mutableListOf<Match>()

        // look for the last 5 matches in the current season
        model.getLatestSeason().let { thisSeason ->
            for (match in thisSeason.getAllMatches()
                // only matches of this club which are finished
                .filter { match -> match.matchOf(club) && match.isFinished() }
                .asReversed()) {
                lis.add(0, match)
                if (lis.size >= five)
                    return lis.asReversed()
            }
        }
        for (m in lis) {
            println(m)
        }
        println("---")
        // add the last matches of the previous season
        if (lis.size < five) {
            val missingMatchDays = five - lis.size
            model.getPreviousSeason()?.let { prev ->
                // the last n matches of the previous season
                val sub = prev.getAllMatches()
                    // only the last matchdays
                    .subList(
                        Matchday.MAX_MATCHDAYS - missingMatchDays,
                        Matchday.MAX_MATCHDAYS
                    )
                for (match in sub
                    // only the matches of this club
                    .filter { match -> match.matchOf(club) }
                    .asReversed()) {
                    lis.add(0, match)
                }
            }
        }

        return lis.asReversed()
    }

}