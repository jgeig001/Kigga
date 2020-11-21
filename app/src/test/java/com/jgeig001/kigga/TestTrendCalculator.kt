package com.jgeig001.kigga

import com.jgeig001.kigga.model.domain.*
import com.jgeig001.kigga.utils.TrendCalculator
import org.junit.Test
import kotlin.math.roundToInt

class TestTrendCalculator {

    @Test
    fun testLast5OneSeason6Matches() {
        val myClub = Club("MyClub", "clb")
        val otherClub = Club("other", "otr")

        val result0 = MatchResult()
        result0.setResults(1, 1, 1, 0)
        val match0 = Match(0, myClub, otherClub, 0, result0)
        val result1 = MatchResult()
        result1.setResults(1, 1, 1, 1)
        val match1 = Match(0, myClub, otherClub, 0, result1)
        val result2 = MatchResult()
        result2.setResults(1, 1, 1, 1)
        val match2 = Match(0, myClub, otherClub, 0, result2)
        val result3 = MatchResult()
        result3.setResults(1, 1, 1, 1)
        val match3 = Match(0, myClub, otherClub, 0, result3)
        val result4 = MatchResult()
        result4.setResults(1, 1, 1, 1)
        val match4 = Match(0, myClub, otherClub, 0, result4)

        val result5 = MatchResult()
        result5.setResults(1, 1, 1, 1)
        val match5 = Match(0, myClub, otherClub, 0, result5)

        val matchday_lis = mutableListOf(
            Matchday(mutableListOf(match0), 1),
            Matchday(mutableListOf(match1), 2),
            Matchday(mutableListOf(match2), 3),
            Matchday(mutableListOf(match3), 4),
            Matchday(mutableListOf(match4), 5),
            Matchday(mutableListOf(match5), 6)
        )

        val season = Season(matchday_lis, 2020)
        val history = History(mutableListOf(season))

        val last5 = TrendCalculator.last5(history, myClub)

        assert(last5[0] == match5)
        assert(last5[1] == match4)
        assert(last5[2] == match3)
        assert(last5[3] == match2)
        assert(last5[4] == match1)
        assert(!last5.contains(match0))
    }

    @Test
    fun testLast5OneSeason5Matches() {
        val myClub = Club("MyClub", "clb")
        val otherClub = Club("other", "otr")

        val result1 = MatchResult()
        result1.setResults(1, 1, 1, 1)
        val match1 = Match(0, myClub, otherClub, 0, result1)
        val result2 = MatchResult()
        result2.setResults(1, 1, 1, 1)
        val match2 = Match(0, myClub, otherClub, 0, result2)
        val result3 = MatchResult()
        result3.setResults(1, 1, 1, 1)
        val match3 = Match(0, myClub, otherClub, 0, result3)
        val result4 = MatchResult()
        result4.setResults(1, 1, 1, 1)
        val match4 = Match(0, myClub, otherClub, 0, result4)
        val result5 = MatchResult()
        result5.setResults(1, 1, 1, 1)
        val match5 = Match(0, myClub, otherClub, 0, result5)

        val matchday_lis = mutableListOf(
            Matchday(mutableListOf(match1), 1),
            Matchday(mutableListOf(match2), 2),
            Matchday(mutableListOf(match3), 3),
            Matchday(mutableListOf(match4), 4),
            Matchday(mutableListOf(match5), 5)
        )

        val season = Season(matchday_lis, 2020)
        val history = History(mutableListOf(season))

        val last5 = TrendCalculator.last5(history, myClub)

        assert(last5[0] == match5)
        assert(last5[1] == match4)
        assert(last5[2] == match3)
        assert(last5[3] == match2)
        assert(last5[4] == match1)
    }

    @Test
    fun testLast5Only3Matches() {
        val myClub = Club("MyClub", "clb")
        val otherClub = Club("other", "otr")

        val result1 = MatchResult()
        result1.setResults(1, 1, 1, 1)
        val match1 = Match(0, myClub, otherClub, 0, result1)
        val result2 = MatchResult()
        result2.setResults(1, 1, 1, 1)
        val match2 = Match(0, myClub, otherClub, 0, result2)
        val result3 = MatchResult()
        result3.setResults(1, 1, 1, 1)
        val match3 = Match(0, myClub, otherClub, 0, result3)


        val matchday_lis = mutableListOf(
            Matchday(mutableListOf(match1), 1),
            Matchday(mutableListOf(match2), 2),
            Matchday(mutableListOf(match3), 3)
        )

        val season = Season(matchday_lis, 2020)
        val history = History(mutableListOf(season))

        val last5 = TrendCalculator.last5(history, myClub)

        assert(last5[0] == match3)
        assert(last5[1] == match2)
        assert(last5[2] == match1)
    }

    @Test
    fun testLast5TwoSeasons() {
        val myClub = Club("MyClub", "clb")
        val otherClub = Club("other", "otr")

        // season0
        val matchday_lis_0 = mutableListOf<Matchday>()
        var i = 1
        while (matchday_lis_0.size < Matchday.MAX_MATCHDAYS - 6) {
            val match = Match(0, myClub, otherClub, 0, MatchResult())
            matchday_lis_0.add(0, Matchday(mutableListOf(match), i))
            i++
        }
        // needed matchdays
        val result00 = MatchResult()
        result00.setResults(1, 1, 29, 0)
        val match29 = Match(0, myClub, otherClub, 0, result00)
        val result10 = MatchResult()
        result10.setResults(1, 1, 30, 1)
        val match30 = Match(0, myClub, otherClub, 0, result10)
        val result20 = MatchResult()
        result20.setResults(1, 1, 31, 1)
        val match31 = Match(0, myClub, otherClub, 0, result20)
        val result30 = MatchResult()
        result30.setResults(1, 1, 21, 1)
        val match32 = Match(0, myClub, otherClub, 0, result30)
        val result40 = MatchResult()
        result40.setResults(1, 1, 33, 1)
        val match33 = Match(0, myClub, otherClub, 0, result40)
        val result50 = MatchResult()
        result50.setResults(1, 1, 34, 1)
        val match34 = Match(0, myClub, otherClub, 0, result50)
        matchday_lis_0.addAll(
            listOf(
                Matchday(mutableListOf(match29), 29),
                Matchday(mutableListOf(match30), 30),
                Matchday(mutableListOf(match31), 31),
                Matchday(mutableListOf(match32), 32),
                Matchday(mutableListOf(match33), 33),
                Matchday(mutableListOf(match34), 34)
            )
        )

        val season0 = Season(matchday_lis_0, 2019)

        // season1
        val result0 = MatchResult()
        result0.setResults(1, 1, 1, 0)
        val match1 = Match(0, myClub, otherClub, 0, result0)
        val result1 = MatchResult()
        result1.setResults(1, 1, 2, 1)
        val match2 = Match(0, myClub, otherClub, 0, result1)
        val result2 = MatchResult()
        result2.setResults(1, 1, 3, 1)
        val match3 = Match(0, myClub, otherClub, 0, result2)

        val matchday_lis_1 = mutableListOf(
            Matchday(mutableListOf(match1), 1),
            Matchday(mutableListOf(match2), 2),
            Matchday(mutableListOf(match3), 3)
        )
        val season1 = Season(matchday_lis_1, 2020)

        // history
        val history = History(mutableListOf(season0, season1))

        val last5 = TrendCalculator.last5(history, myClub)

        for (m in last5) {
            println(m)
        }

        assert(last5[0] == match3)
        assert(last5[1] == match2)
        assert(last5[2] == match1)
        assert(last5[3] == match34)
        assert(last5[4] == match33)
        assert(!last5.contains(match32))
        assert(!last5.contains(match31))
        assert(!last5.contains(match30))
        assert(!last5.contains(match29))


    }

    @Test
    fun testCalcTrendWinOnly() {
        val myClub = Club("MyClub", "clb")
        val otherClub = Club("other", "otr")

        val result1 = MatchResult()
        result1.setResults(1, 1, 10, 1)
        val match1 = Match(0, myClub, otherClub, 0, result1)
        val result2 = MatchResult()
        result2.setResults(1, 1, 10, 1)
        val match2 = Match(0, myClub, otherClub, 0, result2)
        val result3 = MatchResult()
        result3.setResults(1, 1, 10, 1)
        val match3 = Match(0, myClub, otherClub, 0, result3)
        val result4 = MatchResult()
        result4.setResults(1, 1, 10, 1)
        val match4 = Match(0, myClub, otherClub, 0, result4)
        val result5 = MatchResult()
        result5.setResults(1, 1, 10, 1)
        val match5 = Match(0, myClub, otherClub, 0, result5)

        val matchday_lis = mutableListOf(
            Matchday(mutableListOf(match1), 1),
            Matchday(mutableListOf(match2), 2),
            Matchday(mutableListOf(match3), 3),
            Matchday(mutableListOf(match4), 4),
            Matchday(mutableListOf(match5), 5)
        )

        val season = Season(matchday_lis, 2020)
        val history = History(mutableListOf(season))

        val trend = TrendCalculator.calcTrend(history, myClub)
        println(trend)
        assert(trend == 1f)
    }

    @Test
    fun testCalcTrendMixed() {
        val myClub = Club("MyClub", "clb")
        val otherClub = Club("other", "otr")

        val result1 = MatchResult()
        result1.setResults(0, 0, 0, 1) // loss
        val match1 = Match(0, myClub, otherClub, 0, result1)
        val result2 = MatchResult()
        result2.setResults(0, 0, 0, 2) // loss
        val match2 = Match(0, myClub, otherClub, 0, result2)
        val result3 = MatchResult()
        result3.setResults(0, 0, 3, 3) // draw
        val match3 = Match(0, myClub, otherClub, 0, result3)
        val result4 = MatchResult()
        result4.setResults(0, 0, 4, 0) // win
        val match4 = Match(0, myClub, otherClub, 0, result4)
        val result5 = MatchResult()
        result5.setResults(0, 0, 5, 0) // win
        val match5 = Match(0, myClub, otherClub, 0, result5)

        val matchday_lis = mutableListOf(
            Matchday(mutableListOf(match1), 1),
            Matchday(mutableListOf(match2), 2),
            Matchday(mutableListOf(match3), 3),
            Matchday(mutableListOf(match4), 4),
            Matchday(mutableListOf(match5), 5)
        )

        val season = Season(matchday_lis, 2020)
        val history = History(mutableListOf(season))

        val trend = TrendCalculator.calcTrend(history, myClub)
        println(trend)
        assert(trend == 0.67f) // 0.666 round up to 0.67


    }

}