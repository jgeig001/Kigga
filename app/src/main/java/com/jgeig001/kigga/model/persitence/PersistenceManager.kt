package com.jgeig001.kigga.model.persitence

import android.content.Context
import android.util.Log
import com.jgeig001.kigga.database.LocalDatabase
import com.jgeig001.kigga.model.database.entites.*
import com.jgeig001.kigga.model.domain.*
import com.jgeig001.kigga.model.exceptions.DatabaseEmptyWarning
import com.jgeig001.kigga.model.repository.*
import kotlinx.coroutines.*
import java.io.*
import java.util.*
import javax.inject.Inject


interface Persistent {
    /**
     * loads the business model from room db
     */
    fun loadFromDatabase(): ModelWrapper

    /**
     * dumps whole model to room db
     */
    suspend fun dumpDatabase()
}

class PersistenceManager @Inject constructor(private var context: Context, val db: LocalDatabase) :
    Persistent {

    val DB_TAG = "localdb"

    private val SERIALIZE_FILE = "serialized"
    private val MAX_TIME_TO_WAIT = 60
    private val first_init_done = false
    private var dataPoller: DataPoller
    private var model: ModelWrapper

    init {
        model = getEmptyModel()
        dataPoller = DataPoller(model.getHistory(), model.getLiga(), context)


        GlobalScope.launch(Dispatchers.IO) {
            // 1. load data from persitence
            model = try {
                loadFromDatabase()
            } catch (e: DatabaseEmptyWarning) {
                // nothing saved yet
                getEmptyModel()
            }

            // 2. load new data from web
            dataPoller = DataPoller(model.getHistory(), model.getLiga(), context)
            dataPoller.addFirstLoadFinishedCallback {
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
    override fun loadFromDatabase(): ModelWrapper {
        var modelWrapper: ModelWrapper
        runBlocking {
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
            val liga = LigaClass()
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

                        var match_index = 0
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
                            val bet = match.getBet()
                            bet.setHomeGoals(matchEntity.bet_home_goals ?: Match.NO_BET)
                            bet.setAwayGoals(matchEntity.bet_away_goals ?: Match.NO_BET)
                            match.setBet(bet)

                            listOfMatches.add(match)

                            match_index += 1
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

            val history = History(listOfSeasons)

            Log.d("123", "loadDatabase() | END |")
            modelWrapper = ModelWrapper(liga, history)
        }
        return modelWrapper
    }

    /**
     * dumps whole model to room db
     */
    override suspend fun dumpDatabase() {

        Log.d(DB_TAG, "exec dumpDatabase()")

        var startTime = System.nanoTime()

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

        var t = (System.nanoTime() - startTime) / 1000000

        Log.e(DB_TAG, "t = $t ms")

    }

    /**
     * adds an callback which gets called when the model got initialised
     */
    fun addFirstLoadFinishedCallback(callback: () -> Unit) {
        dataPoller.addFirstLoadFinishedCallback(callback)
    }

    fun setFavClubCallback(callback: (liga: LigaClass) -> Unit) {
        dataPoller.setFavClubCallback(callback)
    }

    fun internetWarningDialog(openDialog: () -> Unit) {
        dataPoller.internetWarningDialog(openDialog)
    }

    /* DEPRECATED */
    /**
     * method that deserialize model and returns it
     * @param context
     * @return loaded model: ModelWrapper
     */
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun deserializeBusinessModel(context: Context): ModelWrapper {
        println("###loadSerializedModel()")
        var fis: FileInputStream = context.openFileInput(SERIALIZE_FILE)
        val inputStream = ObjectInputStream(fis)
        val lis = inputStream.readObject() as ArrayList<*>
        inputStream.close()
        fis.close()
        val liga = lis[1] as LigaClass
        val history = lis[2] as History
        return ModelWrapper(liga, history)
    }

    /**
     * Saves the data with java serialization.
     * @param context
     */
    fun serializeBusinessModel(context: Context) {
        println("###SerializedModel()")
        try {
            val fos: FileOutputStream = context.openFileOutput(SERIALIZE_FILE, Context.MODE_PRIVATE)
            val os = ObjectOutputStream(fos)
            val lis = mutableListOf<Any>()
            lis.add(model.getLiga())
            lis.add(model.getHistory())
            os.writeObject(lis)
            os.close()
            fos.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}