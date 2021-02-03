package com.jgeig001.kigga.model.persitence

import android.util.Log
import com.jgeig001.kigga.model.domain.*
import com.jgeig001.kigga.model.domain.Matchday.Companion.MAX_MATCHDAYS
import com.jgeig001.kigga.model.domain.Matchday.Companion.MAX_MATCHES
import com.jgeig001.kigga.model.exceptions.ClubExistenceException
import com.jgeig001.kigga.model.exceptions.NotLoadableException
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

interface Updater {
    fun updateData()
    fun loadTable()
    fun loadNewClubs(): Boolean
    fun getLastUpdateOf(season: Season, matchday: Matchday): Date
}

class DataLoader(private var history: History, private var liga: LigaClass) : Updater {

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
            lastLoadedSeasonYear = this.getCurYear()
        } else {
            lastLoadedSeasonYear = lastLoadedSeason.getYear()
        }
        var year = lastLoadedSeasonYear
        var loadedSeason: Season

        // ----------------- load every whole season after curYear -----------------
        if (lastLoadedSeasonYear < this.getCurYear() || emptyHistory) {
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
        val tup = history.getFirstMatchdayWithMissingResults()
        val matchday_index: Int
        if (tup != null) {
            val firstSeasonWithMissingResults = tup.component1()
            val lastLoadedMatchday = tup.component2()

            // last matchday in numbers
            matchday_index =
                firstSeasonWithMissingResults.getMatchdayIndexOf(lastLoadedMatchday) + 0
            year = firstSeasonWithMissingResults.getYear()
        } else {
            // no results loaded yet: load all results
            matchday_index = 1
            year = this.getCurYear()
        }
        // load new results into map
        var loadedResults: Map<Int, MatchResult>

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
                    loadedResults = createMatchResultObjsFromJSON(matchesAsJSONObjectList)

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

    override fun loadTable() {
        try {
            // fill list with TableElements
            var tmpTableLis: MutableList<TableElement>
            for (season in history.getUnfinishedSeasons()) {
                tmpTableLis = mutableListOf()
                val url = "https://www.openligadb.de/api/getbltable/bl1/${season.getYear()}"
                val jsonArray = JSON_Reader.readJsonFromUrl(url)
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

    private fun getCurYear(): Int {
        val cal = Calendar.getInstance()
        val JULI = 7
        return if (cal[Calendar.MONTH] < JULI) {
            // e.g. JANURARY 2020 is still season 2019
            cal[Calendar.YEAR] - 1
        } else {
            cal[Calendar.YEAR]
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
            val kickoffString = jsonMatchObj.getString("MatchDateTime").replace('T', ' ')
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val date = formatter.parse(kickoffString)
            val kickoff = date.time
            if (matchday.updateKickoff(matchID, kickoff)) {
                changeDone = true
            }
        }
        return changeDone
    }

    /**
     * downloads data and returns a list of JSONObject, each representing a match
     */
    private fun getJsonObjectsOfMatchday(year: Int, matchday_number: Int): List<JSONObject> {
        val matchObjectList = mutableListOf<JSONObject>()
        val url = String.format(
            "https://www.openligadb.de/api/getmatchdata/bl1/%d/%d", year, matchday_number
        )
        // array of match objects: [jsonObj, jsonObj, jsonObj,...]
        val jsonArrayOfMatches = JSON_Reader.readJsonFromUrl(url)
        if (jsonArrayOfMatches != null) {
            for (i in 0 until jsonArrayOfMatches.length()) {
                // get i_th matchObject out of array
                val jsonMatchObj = jsonArrayOfMatches.getJSONObject(i)
                matchObjectList.add(jsonMatchObj)
            }
        }
        return matchObjectList
    }

    /**
     *
     */
    @Throws(NotLoadableException::class)
    private fun createMatchResultObjsFromJSON(matchObjectList: List<JSONObject>): Map<Int, MatchResult> {
        val map = mutableMapOf<Int, MatchResult>()
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
    private fun getResultOf(json_matchObj: JSONObject): MatchResult? {
        val jsonMatchResult = json_matchObj.getJSONArray("MatchResults")
        if (jsonMatchResult.length() != 0) {
            val FULLTIME_INDEX = 1
            val halfTimeTeam1 = jsonMatchResult.getJSONObject(FULLTIME_INDEX).getInt("PointsTeam1")
            val halfTimeTeam2 = jsonMatchResult.getJSONObject(FULLTIME_INDEX).getInt("PointsTeam2")
            val HALFTIME_INDEX = 0
            val fullTimeTeam1 = jsonMatchResult.getJSONObject(HALFTIME_INDEX).getInt("PointsTeam1")
            val fullTimeTeam2 = jsonMatchResult.getJSONObject(HALFTIME_INDEX).getInt("PointsTeam2")
            val matchIsFinished = json_matchObj.getBoolean("MatchIsFinished")
            return MatchResult(
                halfTimeTeam1,
                halfTimeTeam2,
                fullTimeTeam1,
                fullTimeTeam2,
                matchIsFinished
            )
        }
        return null
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
        val url = "https://www.openligadb.de/api/getmatchdata/bl1/$year"
        // parse json
        val allMatches = mutableListOf<Match>()
        val season: Season
        try {
            val jsonArrayOfMatches = JSON_Reader.readJsonFromUrl(url)
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
                var kickoff: Long
                // 1
                val kickoffString = json_match.getString("MatchDateTime").replace('T', ' ')
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val date = formatter.parse(kickoffString)
                kickoff = date.time
                match = Match(matchID, homeTeam, awayTeam, kickoff, MatchResult())

                // what about the result...?
                val jsonMatchResult = json_match.getJSONArray("MatchResults")
                if (jsonMatchResult.length() != 0) {
                    // match has started and a result
                    val halfTimeTeam1 = jsonMatchResult.getJSONObject(1).getInt("PointsTeam1")
                    val halfTimeTeam2 = jsonMatchResult.getJSONObject(1).getInt("PointsTeam2")
                    val fullTimeTeam1 = jsonMatchResult.getJSONObject(0).getInt("PointsTeam1")
                    val fullTimeTeam2 = jsonMatchResult.getJSONObject(0).getInt("PointsTeam2")
                    val matchIsFinished = json_match.getBoolean("MatchIsFinished")
                    val matchResult = MatchResult(
                        halfTimeTeam1,
                        halfTimeTeam2,
                        fullTimeTeam1,
                        fullTimeTeam2,
                        matchIsFinished
                    )
                    match.setResult(matchResult)
                }
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

    override fun getLastUpdateOf(season: Season, matchday: Matchday): Date {
        val url = String.format(
            "https://www.openligadb.de/api/getlastchangedate/bl1/%d/%d",
            season.getYear(), season.getMatchdayIndexOf(matchday) + 1
        )
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
            return Date(Long.MIN_VALUE)
        } catch (e: ParseException) {
            Log.e("123", "ParseException")
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
        val url =
            "https://www.openligadb.de/api/getavailableteams/bl1/${getCurYear()}"
        try {
            val jsonArrayOfClubs = JSON_Reader.readJsonFromUrl(url)
            if (jsonArrayOfClubs == null) {
                return false
            }
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

}