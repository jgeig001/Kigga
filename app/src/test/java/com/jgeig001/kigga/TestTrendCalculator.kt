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
        result0.finishIt()
        val match0 = Match(0, myClub, otherClub, 0, result0)
        val result1 = MatchResult()
        result1.finishIt()
        val match1 = Match(0, myClub, otherClub, 0, result1)
        val result2 = MatchResult()
        result2.finishIt()
        val match2 = Match(0, myClub, otherClub, 0, result2)
        val result3 = MatchResult()
        result3.finishIt()
        val match3 = Match(0, myClub, otherClub, 0, result3)
        val result4 = MatchResult()
        result4.finishIt()
        val match4 = Match(0, myClub, otherClub, 0, result4)
        val result5 = MatchResult()
        result5.finishIt()
        val match5 = Match(0, myClub, otherClub, 0, result5)

        val matchday_lis = mutableListOf(
            Matchday(mutableListOf(match0), 0),
            Matchday(mutableListOf(match1), 1),
            Matchday(mutableListOf(match2), 2),
            Matchday(mutableListOf(match3), 3),
            Matchday(mutableListOf(match4), 4),
            Matchday(mutableListOf(match5), 5)
        )

        val season = Season(matchday_lis, 2020)
        val history = History(mutableListOf(season))
        val model = ModelWrapper(User(null), LigaClass(), history)

        val last5 = TrendCalculator.last5(model, myClub)

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
        result1.finishIt()
        val match1 = Match(0, myClub, otherClub, 0, result1)
        val result2 = MatchResult()
        result2.finishIt()
        val match2 = Match(0, myClub, otherClub, 0, result2)
        val result3 = MatchResult()
        result3.finishIt()
        val match3 = Match(0, myClub, otherClub, 0, result3)
        val result4 = MatchResult()
        result4.finishIt()
        val match4 = Match(0, myClub, otherClub, 0, result4)
        val result5 = MatchResult()
        result5.finishIt()
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
        val model = ModelWrapper(User(null), LigaClass(), history)

        val last5 = TrendCalculator.last5(model, myClub)

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
        result1.finishIt()
        val match1 = Match(0, myClub, otherClub, 0, result1)
        val result2 = MatchResult()
        result2.finishIt()
        val match2 = Match(0, myClub, otherClub, 0, result2)
        val result3 = MatchResult()
        result3.finishIt()
        val match3 = Match(0, myClub, otherClub, 0, result3)


        val matchday_lis = mutableListOf(
            Matchday(mutableListOf(match1), 1),
            Matchday(mutableListOf(match2), 2),
            Matchday(mutableListOf(match3), 3)
        )

        val season = Season(matchday_lis, 2020)
        val history = History(mutableListOf(season))
        val model = ModelWrapper(User(null), LigaClass(), history)

        val last5 = TrendCalculator.last5(model, myClub)

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
        result00.finishIt()
        val match29 = Match(0, myClub, otherClub, 0, result00)
        val result10 = MatchResult()
        result10.finishIt()
        val match30 = Match(0, myClub, otherClub, 0, result10)
        val result20 = MatchResult()
        result20.finishIt()
        val match31 = Match(0, myClub, otherClub, 0, result20)
        val result30 = MatchResult()
        result30.finishIt()
        val match32 = Match(0, myClub, otherClub, 0, result30)
        val result40 = MatchResult()
        result40.finishIt()
        val match33 = Match(0, myClub, otherClub, 0, result40)
        val result50 = MatchResult()
        result50.finishIt()
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
        result0.finishIt()
        val match1 = Match(0, myClub, otherClub, 0, result0)
        val result1 = MatchResult()
        result1.finishIt()
        val match2 = Match(0, myClub, otherClub, 0, result1)
        val result2 = MatchResult()
        result2.finishIt()
        val match3 = Match(0, myClub, otherClub, 0, result2)

        val matchday_lis_1 = mutableListOf(
            Matchday(mutableListOf(match1), 1),
            Matchday(mutableListOf(match2), 2),
            Matchday(mutableListOf(match3), 3)
        )
        val season1 = Season(matchday_lis_1, 2020)

        // history
        val history = History(mutableListOf(season0, season1))
        val model = ModelWrapper(User(null), LigaClass(), history)

        val last5 = TrendCalculator.last5(model, myClub)

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

        val result1 = MatchResult(1, 1, 10, 1, true)
        val match1 = Match(0, myClub, otherClub, 0, result1)
        val result2 = MatchResult(1, 1, 10, 1, true)
        val match2 = Match(0, myClub, otherClub, 0, result2)
        val result3 = MatchResult(1, 1, 10, 1, true)
        val match3 = Match(0, myClub, otherClub, 0, result3)
        val result4 = MatchResult(1, 1, 10, 1, true)
        val match4 = Match(0, myClub, otherClub, 0, result4)
        val result5 = MatchResult(1, 1, 10, 1, true)
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
        val model = ModelWrapper(User(null), LigaClass(), history)

        val trend = TrendCalculator.calcTrend(model, myClub)
        println(trend)
        assert(trend == 1f)
    }

    @Test
    fun testCalcTrendMixed() {
        val myClub = Club("MyClub", "clb")
        val otherClub = Club("other", "otr")

        val result1 = MatchResult(0, 0, 0, 1, true) // loss
        val match1 = Match(0, myClub, otherClub, 0, result1)

        val result2 = MatchResult(0, 0, 0, 2, true) // loss
        val match2 = Match(0, myClub, otherClub, 0, result2)

        val result3 = MatchResult(0, 0, 3, 3, true) // draw
        val match3 = Match(0, myClub, otherClub, 0, result3)

        val result4 = MatchResult(0, 0, 4, 0, true) // win
        val match4 = Match(0, myClub, otherClub, 0, result4)

        val result5 = MatchResult(0, 0, 5, 0, true) // win
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
        val model = ModelWrapper(User(null), LigaClass(), history)

        val trend = TrendCalculator.calcTrend(model, myClub)
        println(trend)
        // 0 to 0.0f, 1 to 0.33f, 3 to 1.0f, null to 0f
        // 6f, 4f, 4f, 3f, 3f
        var soll_trend = 1f * 6f + 1f * 4f + 0.33f * 4f + 0f * 3f + 0f * 3f
        soll_trend = soll_trend / 20
        soll_trend = ((soll_trend * 100.0).roundToInt() / 100.0).toFloat()
        assert(trend == soll_trend)


    }

}