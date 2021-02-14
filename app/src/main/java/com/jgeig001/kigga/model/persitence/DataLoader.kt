package com.jgeig001.kigga.model.persitence

import com.jgeig001.kigga.model.domain.*
import com.jgeig001.kigga.model.domain.Matchday.Companion.MAX_MATCHDAYS
import com.jgeig001.kigga.model.domain.Matchday.Companion.MAX_MATCHES
import com.jgeig001.kigga.model.exceptions.ClubExistenceException
import com.jgeig001.kigga.model.exceptions.NotLoadableException
import com.jgeig001.kigga.model.exceptions.ServerConnectionException
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.net.UnknownHostException
import java.nio.charset.Charset
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

interface DataUpdater {
    fun updateData()
    fun updateSuspendedMatches()
    fun loadTable()
    fun loadNewClubs(): Boolean
    fun getLastUpdateOf(seasonYear: Int, matchdayNumber: Int): Date
}

class DataLoader(
    private var history: History,
    private var liga: LigaClass,
    private var api: OpenLigaDB_API
) : DataUpdater {

    private var suspendedMatchesMap: Map<Matchday, List<Match>> = pickSuspendedMatches()
    var curYear: Int = calcCurYear(history)

    private fun pickSuspendedMatches(): Map<Matchday, List<Match>> {
        return history.getRunningSeason()?.getOldSuspendedMatches() ?: mapOf()
    }

    /**
     * loads new available data
     */
    override fun updateData() {
        // --- LOGIC: what to load --- //
        var emptyHistory = false
        val lastLoadedSeason: Season? = try {
            history.getLatestSeason()
        } catch (ex: NoSuchElementException) {
            null
        }
        val lastLoadedSeasonYear: Int
        if (lastLoadedSeason == null) {
            // nothing loaded yet
            emptyHistory = true
            lastLoadedSeasonYear = curYear
        } else {
            lastLoadedSeasonYear = lastLoadedSeason.getYear()
        }
        var year = lastLoadedSeasonYear
        var loadedSeason: Season

        // ----------------- load every whole season after curYear -----------------
        if (lastLoadedSeasonYear < curYear || emptyHistory) {
            while (true) {
                try {
                    loadedSeason = getSeason(year)
                    history.addSeason(loadedSeason)
                } catch (e: NotLoadableException) {
                    // no new data to load
                    break
                }
                year += 1
            }
        } // else { data is up to date }

        // ----------------- load new match results -----------------
        // last matchday with loaded results
        val tuple = history.getFirstMatchdayWithMissingResults()
        val matchday_index: Int
        if (tuple != null) {
            val firstSeasonWithMissingResults = tuple.component1()
            val lastLoadedMatchday = tuple.component2()

            // last matchday in numbers
            matchday_index =
                firstSeasonWithMissingResults.getMatchdayIndexOf(lastLoadedMatchday) + 0
            year = firstSeasonWithMissingResults.getYear()
        } else {
            // no results loaded yet: load all results
            matchday_index = 1
            year = curYear
        }
        // load new results into map
        var loadedResults: Map<Int, FootballMatchResult>

        val allSeasonsIter: List<Season> = history.getSeasonsSince(year)
        var incompletMatchdaysIterForCurSeason: List<Matchday>

        for (curSeason in allSeasonsIter) {
            try {
                incompletMatchdaysIterForCurSeason =
                    curSeason.getMatchdaysSinceIndex(matchday_index)
                // check results and kickoffs for all unfinished matchdays
                for ((matchday, i) in incompletMatchdaysIterForCurSeason.zip(matchday_index until MAX_MATCHDAYS)) {

                    val matchday_number = i + 1
                    val matchesAsJSONObjectList =
                        getJsonObjectsOfMatchday(curSeason.getYear(), matchday_number)

                    // update kickoff data
                    val newKickoffData = refreshKickoffData(matchesAsJSONObjectList, matchday)

                    // load results of the matchday number [matchday_number]
                    loadedResults = createMatchResultMapFromJSON(matchesAsJSONObjectList)

                    // check if any match is rescheduled
                    checkForSuspendedMatches(matchesAsJSONObjectList, matchday)

                    if (loadedResults.isEmpty() && !newKickoffData) {
                        // if there are no new results or kickoffs data: you are finished
                        return
                    }

                    // pass the loaded results to the associated matches
                    for ((matchID, matchResult) in loadedResults) {
                        matchday.setResult(matchID, matchResult)
                    }

                }
            } catch (e: Exception) {
                // something went wrong ?!
                e.printStackTrace()
                break
            }
        }
    }

    /**
     * updates all suspended matches of the current season: kickoff & matchresult
     */
    override fun updateSuspendedMatches() {
        for ((matchday, matchList) in suspendedMatchesMap) {
            // check if new data available
            val lastUpdate = getLastUpdateOf(curYear, matchday.getMatchdayNumber()).time
            val now: Long = Calendar.getInstance().time.time
            val newDataAvailable = lastUpdate > now
            if (newDataAvailable) {
                for (match in matchList) {
                    val jsonMatchObject = getJsonObjectOf(match) ?: continue
                    val kickoff = getKickoffOf(jsonMatchObject)
                    matchday.specifyKickoff(match.matchID, kickoff)
                    val matchResult: FootballMatchResult = getResultOf(jsonMatchObject) ?: continue
                    matchday.setResult(match.matchID, matchResult)
                }
            }
        }
    }

    private fun calcCurYear(history: History): Int {
        if (history.getListOfSeasons().isNotEmpty()) {
            // has loaded some data to work with
            val finishedSeasons = history.getFinishedSeasons()
            if (finishedSeasons.isEmpty()) {
                return history.getListOfSeasons().first().getYear()
            }
            return finishedSeasons.last().getYear() + 1
        } else {
            // no data loaded yet
            val cal = Calendar.getInstance()
            val JULI = 7
            return if (cal[Calendar.MONTH] < JULI) {
                // e.g. JANURARY 2020 is still season 2019
                cal[Calendar.YEAR] - 1
            } else {
                cal[Calendar.YEAR]
            }
        }
    }

    /**
     * updates kickoff data: concrete kickoff data is just released iteratively by 'DFL'
     * @param matchesAsJSONObjectList: list of JSONObjects repr a match
     * @returns true if any update was made, false if this concrete data was set already
     */
    private fun refreshKickoffData(
        matchesAsJSONObjectList: List<JSONObject>,
        matchday: Matchday
    ): Boolean {
        var changeDone = false
        if (matchday.kickoffDiff()) {
            // return if matches have different kickoffs
            return changeDone
        }
        val matchIdList = mutableListOf<Int>()
        for (jsonMatchObj in matchesAsJSONObjectList) {
            val matchID = jsonMatchObj.getInt("MatchID")
            matchIdList.add(matchID)
            val kickoff = getKickoffOf(jsonMatchObj)
            if (matchday.specifyKickoff(matchID, kickoff)) {
                changeDone = true
            }
        }
        return changeDone
    }

    /**
     * downloads data and returns a JSONObject representing a match
     * 58751
     */
    private fun getJsonObjectOf(match: Match): JSONObject? {
        val url = String.format(api.URL_MATCH_ID, match.matchID)
        val matchObject = JSON_Reader.readJsonObjectFromUrl(url)
        if (matchObject != null && !matchObject.isNull("Team1")) {
            return matchObject
        }
        return null
    }

    /**
     * downloads data and returns a list of JSONObject, each representing a match
     */
    private fun getJsonObjectsOfMatchday(year: Int, matchday_number: Int): List<JSONObject> {
        val matchObjectList = mutableListOf<JSONObject>()
        val url = String.format(
            api.URL_MATCHDAY, year, matchday_number
        )
        // array of match objects: [jsonObj, jsonObj, jsonObj,...]
        val jsonArrayOfMatches = JSON_Reader.readJsonArrayFromUrl(url)
        if (jsonArrayOfMatches != null) {
            for (i in 0 until jsonArrayOfMatches.length()) {
                // get i_th matchObject out of array
                val jsonMatchObj = jsonArrayOfMatches.getJSONObject(i)
                matchObjectList.add(jsonMatchObj)
            }
        }
        return matchObjectList
    }

    private fun createMatchResultMapFromJSON(matchObjectList: List<JSONObject>): Map<Int, FootballMatchResult> {
        val map = mutableMapOf<Int, FootballMatchResult>()
        for (jsonMatchObj in matchObjectList) {
            val matchResult = getResultOf(jsonMatchObj)
            if (matchResult != null) {
                val matchID = jsonMatchObj.getInt("MatchID")
                map[matchID] = matchResult
            }
        }
        return map
    }

    /**
     * returns a MatchResult object of the match represented by the [json_matchObj]
     * returns null if no result available
     */
    private fun getResultOf(json_matchObj: JSONObject): FootballMatchResult? {
        val jsonMatchResult = json_matchObj.getJSONArray("MatchResults")
        if (jsonMatchResult.length() != 0) {
            val FULLTIME_INDEX = 1
            val halfTimeTeam1 = jsonMatchResult.getJSONObject(FULLTIME_INDEX).getInt("PointsTeam1")
            val halfTimeTeam2 = jsonMatchResult.getJSONObject(FULLTIME_INDEX).getInt("PointsTeam2")
            val HALFTIME_INDEX = 0
            val fullTimeTeam1 = jsonMatchResult.getJSONObject(HALFTIME_INDEX).getInt("PointsTeam1")
            val fullTimeTeam2 = jsonMatchResult.getJSONObject(HALFTIME_INDEX).getInt("PointsTeam2")
            val matchIsFinished = json_matchObj.getBoolean("MatchIsFinished")
            return FootballMatchResult(
                halfTimeTeam1,
                halfTimeTeam2,
                fullTimeTeam1,
                fullTimeTeam2,
                matchIsFinished
            )
        }
        return null
    }

    private fun getKickoffOf(jsonMatchObj: JSONObject): Long {
        val kickoffString = jsonMatchObj.getString("MatchDateTime").replace('T', ' ')
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = formatter.parse(kickoffString)
        return date.time
    }

    /**
     * mark all suspended matches as rescheduled
     */
    fun checkForSuspendedMatches(matchesAsJSONObjectList: List<JSONObject>, matchday: Matchday) {
        for (jsonMatchObj in matchesAsJSONObjectList) {
            val locationCity = try {
                jsonMatchObj.getJSONObject("Location").getString("LocationCity")
            } catch (e: JSONException) {
                continue
            }
            // depends on user made database
            val reasonsList = listOf("Spiel ausgesetzt", "termin")
            var matchIsSuspended = false
            for (reason in reasonsList) {
                if (locationCity.contains(reason, ignoreCase = true)) { // substring
                    matchIsSuspended = true
                    break
                }
            }
            if (matchIsSuspended) {
                val matchID = jsonMatchObj.getInt("MatchID")
                matchday.markAsRescheduled(matchID)
            }
        }
    }

    /**
     * Loads the whole season of [year]
     *
     * @param year: Int
     * @return season: Season
     * @throws NotLoadableException
     */
    @Throws(NotLoadableException::class)
    private fun getSeason(year: Int): Season {
        // get json as string
        val url = String.format(api.URL_SEASON, year)
        // parse json
        val allMatches = mutableListOf<Match>()
        val season: Season
        try {
            val jsonArrayOfMatches = JSON_Reader.readJsonArrayFromUrl(url)
            if (jsonArrayOfMatches == null || jsonArrayOfMatches.length() == 0) {
                throw NotLoadableException(String.format("season %d is not available ", year))
            }

            // fill allMatches
            for (i in 0 until jsonArrayOfMatches.length()) {
                val json_match = jsonArrayOfMatches.getJSONObject(i)
                var match: Match

                // matchId...
                val matchID: Int = json_match.getInt("MatchID")

                // Clubs...
                val homeTeam = getClubFrom(json_match.getJSONObject("Team1"))
                val awayTeam = getClubFrom(json_match.getJSONObject("Team2"))

                // kickoff...
                val kickoff = getKickoffOf(json_match)

                match = Match(
                    matchID,
                    homeTeam,
                    awayTeam,
                    kickoff
                )

                // what about the result...?
                val matchResult = getResultOf(json_match)
                if (matchResult != null)
                    match.setResult(matchResult)

                // add match
                allMatches.add(match)
            }
            // generates Season from list of all matches
            val all_matchdays = mutableListOf<Matchday>()
            var cur_matchday_matches: MutableList<Match>
            var i = 0
            while (i < MAX_MATCHDAYS * MAX_MATCHES) {
                cur_matchday_matches = mutableListOf()
                for (j in 0 until MAX_MATCHES) {
                    cur_matchday_matches.add(allMatches[i + j])
                }
                val matchday = Matchday(cur_matchday_matches, i / MAX_MATCHES)
                all_matchdays.add(matchday)
                i += MAX_MATCHES
            }
            season = Season(all_matchdays, year)
        } catch (e: JSONException) {
            e.printStackTrace()
            throw NotLoadableException("loading season failed")
        } catch (e: IOException) {
            e.printStackTrace()
            throw NotLoadableException("loading season failed")
        } catch (e: ParseException) {
            e.printStackTrace()
            throw NotLoadableException("loading season failed")
        }
        // finally return the season
        return season
    }

    /**
     * check if new data available to prevent unnecessary polling
     */
    override fun getLastUpdateOf(seasonYear: Int, matchdayNumber: Int): Date {
        val url = String.format(api.URL_CHANGE, seasonYear, matchdayNumber)
        var inputStream: InputStream? = null
        try {
            val connection = URL(url).openConnection()
            connection.readTimeout = 30000 // 30 sec
            inputStream = connection.getInputStream()
            val rd = BufferedReader(InputStreamReader(inputStream, Charset.forName("UTF-8")))
            var jsonText = JSON_Reader.readAll(rd)
            jsonText = jsonText.replace('T', ' ').dropLast(1).drop(1)
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS")
            return formatter.parse(jsonText)
        } catch (e: UnknownHostException) {
            throw ServerConnectionException("unable to reach backend server")
        } catch (e: ParseException) {
            return Date(Long.MIN_VALUE)
        } finally {
            inputStream?.close()
        }
    }

    @Throws(JSONException::class)
    private fun getClubFrom(teamObject: JSONObject): Club {
        return try {
            liga.getClubBy(teamObject.getString("TeamName"))
        } catch (e: ClubExistenceException) {
            val newClub = Club(teamObject.getString("TeamName"), teamObject.getString("ShortName"))
            liga.addClub(newClub)
            newClub
        }
    }

    /**
     * adds (new) club to Liga object
     * returns true if successful else false
     */
    override fun loadNewClubs(): Boolean {
        val url = String.format(api.URL_CLUBS, curYear)
        try {
            val jsonArrayOfClubs = JSON_Reader.readJsonArrayFromUrl(url) ?: return false
            for (i in 0 until jsonArrayOfClubs.length()) {
                val jsonClubObj = jsonArrayOfClubs.getJSONObject(i)
                if (!liga.clubExists(jsonClubObj.getString("TeamName"))) {
                    liga.addClub(
                        Club(
                            jsonClubObj.getString("TeamName"),
                            jsonClubObj.getString("ShortName")
                        )
                    )
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        } catch (e: JSONException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    override fun loadTable() {
        try {
            // fill list with TableElements
            var tmpTableLis: MutableList<TableElement>
            for (season in history.getUnfinishedSeasons()) {
                tmpTableLis = mutableListOf()
                val url = String.format(api.URL_TABLE, season.getYear())
                val jsonArray = JSON_Reader.readJsonArrayFromUrl(url)
                if (jsonArray != null) {
                    for (i in 0 until jsonArray.length()) {
                        /* iterate over teams */
                        val jsonTeamObj = jsonArray.getJSONObject(i)
                        // get club object
                        jsonTeamObj["TeamName"]
                        val club = try {
                            liga.getClubBy(jsonTeamObj.getString("TeamName"))
                        } catch (ex: ClubExistenceException) {
                            this.getClubFrom(jsonTeamObj)
                        }
                        if (season.getTable().isComplete()) {
                            season.getTable().clearTable()
                        }
                        // TODO: better hide TableElement type in Table class, use some caching foobar #cleancode
                        tmpTableLis.add(
                            TableElement(
                                club,
                                jsonTeamObj.getInt("Points"),
                                jsonTeamObj.getInt("Goals"),
                                jsonTeamObj.getInt("OpponentGoals"),
                                jsonTeamObj.getInt("Won"),
                                jsonTeamObj.getInt("Draw"),
                                jsonTeamObj.getInt("Lost"),
                                jsonTeamObj.getInt("Matches")
                            )
                        )
                    }
                    season.setTableList(tmpTableLis)
                }
            }
        } catch (ex: Exception) {
            // return, at least you tried
            ex.printStackTrace()
            return
        }
    }

}