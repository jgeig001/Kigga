package com.jgeig001.kigga.model

import com.jgeig001.kigga.model.domain.Club
import com.jgeig001.kigga.model.domain.Match
import com.jgeig001.kigga.model.domain.Matchday
import com.jgeig001.kigga.model.domain.Season
import org.junit.Test

class SeasonTest {

    lateinit var season: Season
    val thisYear = 2021

    @Test
    fun testSeason() {
        val pair = createDummySeason()
        season = pair.first
        val matchdayList = pair.second
        assert(season.getYear() == thisYear)
        assert(season.getAllMatches().size == Matchday.MAX_MATCHDAYS * Matchday.MAX_MATCHES)
        assert(season.getCurrentMatchday() == matchdayList[0])
        assert(season.getFinishedMatches().isEmpty())
        assert(!season.isFinished())
        assert(season.getTable().tableList.isEmpty())
        //assert(season.getMatchday(33))
    }

    private fun createDummySeason(): Pair<Season, List<Matchday>> {
        val matchdays = mutableListOf<Matchday>()
        for (i in 0 until Matchday.MAX_MATCHDAYS) {
            val matchList = mutableListOf<Match>()
            for (j in 0 until Matchday.MAX_MATCHES) {
                val x = numCat(i, j)
                matchList.add(
                    Match(
                        x,
                        Club("h$x", "h$x"),
                        Club("a$x", "a$x"),
                        x.toLong()
                    )
                )
            }
            matchdays.add(Matchday(matchList, i))
        }
        return Pair(Season(matchdays, thisYear), matchdays)
    }

    private fun numCat(i1: Int, i2: Int): Int {
        return "$i1$i2".toInt()
    }

}