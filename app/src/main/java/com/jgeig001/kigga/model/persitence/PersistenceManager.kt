package com.jgeig001.kigga.model.persitence

import android.content.Context
import android.util.Log
import com.jgeig001.kigga.database.LocalDatabase
import com.jgeig001.kigga.model.database.entites.*
import com.jgeig001.kigga.model.domain.*
import com.jgeig001.kigga.model.exceptions.DatabaseEmptyWarning
import com.jgeig001.kigga.model.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


interface Persistent {
    /**
     * loads the business model from room db
     */
    fun loadFromDatabase(model: ModelWrapper)

    /**
     * dumps whole model to room db
     */
    suspend fun dumpDatabase()
}

class PersistenceManager @Inject constructor(private var context: Context, val db: LocalDatabase) :
    Persistent {

    val DB_TAG = "localdb"

    private var dataPoller: DataPoller
    private var model: ModelWrapper

    init {
        model = getEmptyModel()
        dataPoller = DataPoller(model.getHistory(), model.getLiga())
        loadModel()
    }

    private fun loadModel() {
        GlobalScope.launch(Dispatchers.IO) {
            // 1. load data from db
            try {
                loadFromDatabase(model)
            } catch (e: DatabaseEmptyWarning) {
                // nothing saved yet
                getEmptyModel()
            }

            // 2. load new data from web
            dataPoller.addDumpDBCallback {
                GlobalScope.launch { dumpDatabase() }
            }
            dataPoller.poll()
        }
    }

    private fun getEmptyModel(): ModelWrapper {
        val liga = LigaClass()
        val history = History()
        return ModelWrapper(liga, history)
    }

    fun getLoadedModel(): ModelWrapper {
        return this.model
    }

    /**
     * loads the business model from room db
     */
    @Throws(DatabaseEmptyWarning::class)
    override fun loadFromDatabase(model: ModelWrapper) {
        runBlocking {
            val startTime = System.nanoTime()
            Log.d("123", "startTime: $startTime")
            val TAG = "TAG"
            Log.d(TAG, "loadDatabase()")
            val clubRepo = ClubRepository(db)
            val seasonRepo = SeasonRepository(db)
            val matchdayRepo = MatchdayRepository(db)
            val matchRepo = MatchRepository(db)
            val tableElementRepo = TableElementRepository(db)

            val db_is_empty = seasonRepo.getAllSeasons().isEmpty()
            if (db_is_empty) {
                throw DatabaseEmptyWarning("database seems to bet empty")
            }

            /* LIGA */
            val liga = model.getLiga()
            for (clubEntity in clubRepo.getAllClubs()) {
                val club =
                    Club(clubEntity.clubName, clubEntity.shortName, clubEntity.twitterHashtag)
                liga.addClub(club)
            }

            /* MATCHES */
            val listOfSeasons = mutableListOf<Season>()
            for (seasonEntity in seasonRepo.getAllSeasons()) {

                val listOfMatchdays = mutableListOf<Matchday>()
                val seasonID = seasonEntity.year

                matchdayRepo.getAllMatchdaysOfSeason(seasonID)
                    .forEachIndexed { index, matchdayEntity ->

                        val listOfMatches = mutableListOf<Match>()
                        val matchdayID = matchdayEntity.matchdayID

                        for (matchEntity in matchRepo.getMatchesOfMatchday(matchdayID)) {
                            // MatchEntity to Match
                            val match = Match(
                                matchEntity.matchID,
                                liga.getClubBy(matchEntity.homeTeamName),
                                liga.getClubBy(matchEntity.awayTeamName),
                                matchEntity.kickoff,
                                MatchResult(
                                    matchEntity.home_halftime,
                                    matchEntity.away_halftime,
                                    matchEntity.home_fulltime,
                                    matchEntity.away_fulltime,
                                    matchEntity.isFinished
                                )
                            )
                            match.setHomeGoals(matchEntity.bet_home_goals ?: Match.NO_BET)
                            match.setAwayGoals(matchEntity.bet_away_goals ?: Match.NO_BET)

                            listOfMatches.add(match)
                        }

                        val matchday = Matchday(listOfMatches, index)
                        listOfMatchdays.add(matchday)
                    }

                val season = Season(listOfMatchdays, seasonID)
                listOfSeasons.add(season)

                /* TABLE */
                val tableElementEntities = tableElementRepo.getTablelementsOf(seasonID)
                val tableElements = tableElementEntities.map { tableElementEntity ->
                    TableElement(
                        liga.getClubBy(tableElementEntity.clubName),
                        tableElementEntity.points,
                        tableElementEntity.goals,
                        tableElementEntity.opponentGoals,
                        tableElementEntity.won,
                        tableElementEntity.draw,
                        tableElementEntity.lost,
                        tableElementEntity.matches
                    )
                }
                season.getTable().setNewTableList(tableElements)
            }

            model.getHistory().setListOfSeasons(listOfSeasons)
            //delay(2000)
        }
    }

    /**
     * dumps whole model to room db
     */
    override suspend fun dumpDatabase() {

        Log.d(DB_TAG, "exec dumpDatabase()")

        val startTime = System.nanoTime()

        val clubRepo = ClubRepository(db)
        val seasonRepo = SeasonRepository(db)
        val matchdayRepo = MatchdayRepository(db)
        val matchRepo = MatchRepository(db)
        val tableRepo = TableRepository(db)
        val tableElementRepo = TableElementRepository(db)

        /* clubs */
        for (club in model.getLiga().getAllClubs()) {
            val clubEntity = ClubEntity(club.clubName, club.shortName, club.twitterHashtag)
            clubRepo.upsert(clubEntity)
        }

        /* matches */
        val history = model.getHistory()
        for (season in history.getListOfSeasons()) {
            // SeasonEntity
            val seasonEntity = SeasonEntity(season.getYear())
            seasonRepo.upsertSeason(seasonEntity)
            val seasonID = seasonEntity.year
            // MatchdayEntity
            for (matchday in season.getMatchdays()) {
                val matchdayEntity =
                    MatchdayEntity(matchday.get_DB_ID(), matchday.matchdayIndex, seasonID)
                matchdayRepo.upsertMatchday(matchdayEntity)
                // MatchEntity
                matchday.matches.forEachIndexed { index, match ->
                    val matchEntity = MatchEntity(
                        match.matchID,
                        index,
                        match.home_team.clubName,
                        match.away_team.clubName,
                        match.getKickoff(),
                        match.getMatchResult().getHalftimeHome(),
                        match.getMatchResult().getHalftimeAway(),
                        match.getMatchResult().getFulltimeHome(),
                        match.getMatchResult().getFulltimeAway(),
                        match.isFinished(),
                        match.getBetHomeGoals(),
                        match.getBetAwayGoals(),
                        matchday.get_DB_ID()
                    )
                    matchRepo.upsertMatch(matchEntity)
                }
            }

            /* table */
            // TableEntity
            val tableEntity = TableEntity(seasonID)
            tableRepo.upsertTable(tableEntity)
            val table = season.getTable()
            table.tableList.forEachIndexed { index, ele ->
                val tableElementEntity = TableElementEntity(
                    "${season.getYear()}${index + 1}".toInt(),
                    ele.club.clubName,
                    index + 1,
                    ele.points,
                    ele.goals,
                    ele.opponentGoals,
                    ele.won,
                    ele.draw,
                    ele.lost,
                    ele.matches,
                    tableEntity.seasonID
                )
                tableElementRepo.upsertTableElement(tableElementEntity)
            }
        }

        val t = (System.nanoTime() - startTime) / 1000000

        Log.e(DB_TAG, "t = $t ms")

    }

    fun setFavClubCallback(callback: (liga: LigaClass) -> Unit) {
        dataPoller.setFavClubCallback(callback)
    }

    fun internetWarningDialog(openDialog: () -> Unit) {
        dataPoller.internetWarningDialog(openDialog)
    }

    fun addBetFragmentCallback(callback: () -> Unit) {
        dataPoller.addBetFragmentCallback(callback)
    }

}