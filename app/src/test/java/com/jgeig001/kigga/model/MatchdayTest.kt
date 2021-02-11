package com.jgeig001.kigga.model

import com.jgeig001.kigga.model.domain.Club
import com.jgeig001.kigga.model.domain.Match
import com.jgeig001.kigga.model.domain.Matchday
import org.junit.Test

class MatchdayTest {

    @Test
    fun testMatchdayEquals() {
        val matchesList1 = mutableListOf<Match>()
        val matchesList2 = mutableListOf<Match>()
        for (i in 0 until Matchday.MAX_MATCHES) {
            val match = Match(i, Club("home", "home"), Club("away", "away"), i.toLong())
            matchesList1.add(match)
            matchesList2.add(match)
        }
        val matchday0 = Matchday(matchesList1, 0)
        matchesList2[0] = Match(17, Club("", ""), Club("", ""), 17L)
        val matchday1 = Matchday(matchesList2, 1)
        val matchday2 = Matchday(matchesList1, 2)

        assert(matchday0 == matchday0)
        assert(matchday1 == matchday1)
        assert(matchday2 == matchday2)
        assert(!matchday0.equals("str"))
        assert(!matchday0.equals(matchday0.matches.first()))
        assert(matchday1 != matchday0)
        assert(matchday0 != matchday2)
        assert(matchday1 != matchday2)
    }

    @Test
    fun testMatchdayIndex() {
        val matchesList = mutableListOf<Match>()
        for (i in 0 until Matchday.MAX_MATCHES) {
            val match = Match(i, Club("home", "home"), Club("away", "away"), i.toLong())
            matchesList.add(match)
        }
        val zero = 0
        val matchday = Matchday(matchesList, zero)
        assert(matchday.matchdayIndex == zero)
        assert(matchday.getMatchdayNumber() == zero + 1)
    }

    @Test
    fun testMatchdayKickoff() {
        // diff
        var matchesList = mutableListOf<Match>()
        for (i in 0 until Matchday.MAX_MATCHES) {
            val match = Match(i, Club("home", "home"), Club("away", "away"), i.toLong())
            matchesList.add(match)
        }
        val matchdayDiff = Matchday(matchesList, 0)
        assert(matchdayDiff.kickoffDiff())

        // same
        matchesList = mutableListOf()
        for (i in 0 until Matchday.MAX_MATCHES) {
            val match = Match(i, Club("home", "home"), Club("away", "away"), 0L)
            matchesList.add(match)
        }
        val matchdaySame = Matchday(matchesList, 0)
        assert(!matchdaySame.kickoffDiff())

        // mixed
        matchesList = mutableListOf()
        for (i in 0 until Matchday.MAX_MATCHES) {
            val kickoff = if (i < 5) 0L else 1L
            val match = Match(i, Club("home", "home"), Club("away", "away"), kickoff)
            matchesList.add(match)
        }
        val matchdayMixed = Matchday(matchesList, 0)
        assert(matchdayMixed.kickoffDiff())
    }



}