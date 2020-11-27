package com.jgeig001.kigga.model.persitence;

import android.util.Log
import com.jgeig001.kigga.model.domain.*
import com.jgeig001.kigga.model.domain.Liga
import com.jgeig001.kigga.model.domain.Matchday.Companion.MAX_MATCHDAYS
import com.jgeig001.kigga.model.domain.Matchday.Companion.MAX_MATCHES
import com.jgeig001.kigga.model.exceptions.ClubExistenceException
import com.jgeig001.kigga.model.exceptions.NotLoadableException
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.nio.charset.Charset
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

interface Updater {
    fun updateData()
    fun loadTable()
    fun loadNewClubs()
    fun getLastUpdateOf(season: Season, matchday: Matchday): Date
}

class DataLoader(private var history: History) : Updater {

    /**
     * loads new available data
     */
    override fun updateData() {
        // --- LOGIC: what to load --- //
        if (history.getListOfSeasons().size == 0) {
            Log.d("123", "model was not load correctly ?")
        }
        var emptyHistory = false
        val lastLoadedSeason = history.getLatestSeason()
        val lastLoadedSeasonYear: Int
        if (lastLoadedSeason == null) {
            // nothing loaded yet
            emptyHistory = true
            lastLoadedSeasonYear = this.getCurYear()
        } else {
            lastLoadedSeasonYear = lastLoadedSeason.getYear()
        }
        var year = lastLoadedSeasonYear
        var loadedSeason: Season?

        // ----------------- load every whole season after curYear -----------------
        if (lastLoadedSeasonYear < this.getCurYear() || emptyHistory) {
            while (true) {
                try {
                    loadedSeason = getSeason(year)
                    history.addSeason(loadedSeason!!)
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
        var matchday_index: Int
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
                // check results for all unfinished matchdays
                for ((matchday, i) in incompletMatchdaysIterForCurSeason.zip(matchday_index until MAX_MATCHDAYS)) {
                    val matchday_number = i + 1
                    // load results of the matchday number [matchday_number]
                    loadedResults = loadNewResults(curSeason.getYear(), matchday_number)
                    if (loadedResults.isEmpty()) {
                        // if there are no new results: you are finished
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
        val x = history.getUnfinishedSeasons()
        for (season in x) {
            val url = "https://www.openligadb.de/api/getbltable/bl1/${season.getYear()}"
            val jsonArray = JSON_Reader.readJsonFromUrl(url)
            for (i in 0 until jsonArray!!.length()) {
                /* iterate over teams */
                val jsonTeamObj = jsonArray.getJSONObject(i)
                // get club object
                jsonTeamObj["TeamName"]
                val club = try {
                    Liga.getClubBy(jsonTeamObj.getString("TeamName"))
                } catch (ex: ClubExistenceException) {
                    this.getClubFrom(jsonTeamObj)
                }
                season.addTeamToTable(
                    club!!,
                    jsonTeamObj.getInt("Points"),
                    jsonTeamObj.getInt("Goals"),
                    jsonTeamObj.getInt("OpponentGoals"),
                    jsonTeamObj.getInt("Won"),
                    jsonTeamObj.getInt("Draw"),
                    jsonTeamObj.getInt("Lost"),
                    jsonTeamObj.getInt("Matches")
                )
            }
            season.printTable()
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
     * returns a list of ALL results of the [matchday_number]. matchday of the season of [year]
     *
     * @param year
     * @param matchday_number
     * @return
     * @throws NotLoadableException
     */
    @Throws(NotLoadableException::class)
    private fun loadNewResults(year: Int, matchday_number: Int): Map<Int, MatchResult> {
        val map = mutableMapOf<Int, MatchResult>()
        try {
            val url = String.format(
                "https://www.openligadb.de/api/getmatchdata/bl1/%d/%d",
                year,
                matchday_number
            )
            val jsonArrayOfMatches = JSON_Reader.readJsonFromUrl(url)
            for (i in 0 until jsonArrayOfMatches!!.length()) {
                val json_match = jsonArrayOfMatches.getJSONObject(i)
                var matchID: Int
                matchID = json_match.getInt("MatchID")
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
                    map[matchID] = matchResult
                }
            }
            return map
        } catch (e: NotLoadableException) {
            // no data available
            throw NotLoadableException("no more results for matchday number: $matchday_number")
        } catch (e: JSONException) {
            e.printStackTrace()
            throw NotLoadableException("no more results for matchday number: $matchday_number")
        } catch (e: IOException) {
            throw NotLoadableException("no more results for matchday number: $matchday_number")
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
    private fun getSeason(year: Int): Season? {
        // get json as string
        val url = "https://www.openligadb.de/api/getmatchdata/bl1/$year"
        // parse json
        val allMatches = mutableListOf<Match>()
        val season: Season
        try {
            val jsonArrayOfMatches = JSON_Reader.readJsonFromUrl(url)
            if (jsonArrayOfMatches!!.isNull(0)) {
                throw NotLoadableException(String.format("season %d is not available", year))
            }

            // fill allMatches
            for (i in 0 until jsonArrayOfMatches.length()) {
                val json_match = jsonArrayOfMatches.getJSONObject(i)
                var match: Match

                // matchId...
                var matchID: Int = json_match.getInt("MatchID")

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
                match = Match(matchID, homeTeam!!, awayTeam!!, kickoff, MatchResult())

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

    /**
     * TODO: use it !!!
     */
    override fun getLastUpdateOf(season: Season, matchday: Matchday): Date {
        try {
            val url = String.format(
                "https://www.openligadb.de/api/getlastchangedate/bl1/%d/%d",
                season.getYear(), season.getMatchdayIndexOf(matchday)
            )
            val `is` = URL(url).openStream()
            val rd = BufferedReader(InputStreamReader(`is`, Charset.forName("UTF-8")))
            var jsonText = JSON_Reader.readAll(rd)
            jsonText = jsonText.replace('T', ' ').dropLast(1).drop(1)
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.ss")
            return formatter.parse(jsonText)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return Date(Long.MIN_VALUE)
    }

    @Throws(JSONException::class)
    private fun getClubFrom(teamObject: JSONObject): Club? {
        return try {
            Liga.getClubBy(teamObject.getString("TeamName"))
        } catch (e: ClubExistenceException) {
            val newClub = Club(teamObject.getString("TeamName"), teamObject.getString("ShortName"))
            Liga.addClub(newClub)
            newClub
        }
    }

    override fun loadNewClubs() {
        val url =
            "https://www.openligadb.de/api/getavailableteams/bl1/${getCurYear()}"
        try {
            val jsonArrayOfClubs = JSON_Reader.readJsonFromUrl(url)
            for (i in 0 until jsonArrayOfClubs!!.length()) {
                val jsonClubObj = jsonArrayOfClubs.getJSONObject(i)
                if (!Liga.clubExists(jsonClubObj.getString("TeamName"))) {
                    Liga.addClub(
                        Club(
                            jsonClubObj.getString("TeamName"),
                            jsonClubObj.getString("ShortName")
                        )
                    )
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

}