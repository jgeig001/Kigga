package com.jgeig001.kigga.model.domain

import android.content.Context
import androidx.databinding.BaseObservable
import com.jgeig001.kigga.model.exceptions.ClubExistenceException
import com.jgeig001.kigga.utils.FavClubChooser
import java.io.Serializable
import javax.inject.Inject

class ModelWrapper @Inject constructor(
    private var liga: LigaClass,
    private var history: History
) : Serializable, BaseObservable(), ModelAPI {

    // -------------------------------------------- API --------------------------------------------

    override fun getPointsCurSeason(): Int {
        history.getRunningSeason()?.let { season ->
            return getPointsOf(season)
        } ?: return 0
    }

    override fun getPointsAllTime(): Int {
        var sum = 0
        for (season in history.getListOfSeasons()) {
            sum += this.getPointsOf(season)
        }
        return sum
    }

    private fun getPointsOf(season: Season): Int {
        var sum = 0
        for (match in season.getAllMatches()) {
            sum += match.getPoints() ?: 0
        }
        return sum
    }

    override fun getFavouriteClub(context: Context): Club? {
        return try {
            FavClubChooser.getFavClub(context, liga)
        } catch (e: ClubExistenceException) {
            null
        }
    }

    override fun setFavouriteClub(context: Context, clubName: String) {
        FavClubChooser.setFavClub(context, clubName)
    }

    override fun getListOfSeasons(): MutableList<Season> {
        return this.history.getListOfSeasons()
    }

    override fun getLatestSeason(): Season {
        return this.history.getLatestSeason()
    }

    override fun getRunningSeason(): Season? {
        return this.history.getRunningSeason()
    }

    override fun getPrevRunningSeason(): Season? {
        return history.getPrevRunningSeason()
    }

    override fun getCurrentMatchday(selectedSeasonIndex: Int): Matchday? {
        return history.getCurrentMatchday(selectedSeasonIndex)
    }

    override fun get_nth_season(n: Int): Season? {
        return this.history.get_nth_season(n)
    }

    override fun getBets(): MutableList<Bet> {
        return mutableListOf()
    }

    // ---

    fun getAllSeasonsDistributionMap(): MutableMap<BetPoints, Int> {
        val allSeasonsMap = mutableMapOf(
            BetPoints.RIGHT_RESULT to 0,
            BetPoints.RIGHT_OUTCOME to 0,
            BetPoints.WRONG to 0
        )
        for (season in getListOfSeasons()) {
            val oneSeasonMap = getBetDistribution(season)
            for ((k, v) in oneSeasonMap) {
                incMap(allSeasonsMap, k, v)
            }
        }
        return allSeasonsMap
    }

    /**
     * returns a map with the distribution of bet results of [season]
     * {RIGHT_RESULT: x, RIGHT_OUTCOME: y, WRONG: n-x-y}
     */
    fun getBetDistribution(season: Season): MutableMap<BetPoints, Int> {
        val map = mutableMapOf(
            BetPoints.RIGHT_RESULT to 0,
            BetPoints.RIGHT_OUTCOME to 0,
            BetPoints.WRONG to 0
        )
        for (match in season.getFinishedMatches()) {
            when (match.getBetPoints()) {
                BetPoints.RIGHT_RESULT -> incMap(map, BetPoints.RIGHT_RESULT)
                BetPoints.RIGHT_OUTCOME -> incMap(map, BetPoints.RIGHT_OUTCOME)
                BetPoints.WRONG -> incMap(map, BetPoints.WRONG)
            }
        }
        return map
    }

    private fun incMap(map: MutableMap<BetPoints, Int>, key: BetPoints, x: Int = 1) {
        val v: Int = map[key] ?: 0
        map[key] = v + x
    }

    fun matchesWithBetAllTime(): Int {
        return getListOfSeasons().sumBy { season -> season.matchesWithBetsSize() }
    }

// ---------------------------------------------------------------------------------------------

    // get components
    fun getLiga(): LigaClass {
        return this.liga
    }

    fun getHistory(): History {
        return this.history
    }

}

/**
 * The API with all public functions the model offer
 */
interface ModelAPI {

    fun getPointsCurSeason(): Int

    fun getPointsAllTime(): Int

    fun getFavouriteClub(context: Context): Club?

    fun setFavouriteClub(context: Context, clubName: String)

    fun getListOfSeasons(): MutableList<Season>

    fun getLatestSeason(): Season?

    fun getRunningSeason(): Season?

    fun getPrevRunningSeason(): Season?

    fun getCurrentMatchday(selectedSeasonIndex: Int): Matchday?

    fun get_nth_season(n: Int): Season?

    fun getBets(): MutableList<Bet>

}