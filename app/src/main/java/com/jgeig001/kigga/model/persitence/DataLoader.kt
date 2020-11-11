package com.jgeig001.kigga.model.persitence;

import com.jgeig001.kigga.model.domain.*
import com.jgeig001.kigga.model.domain.Liga.Companion.addClub
import com.jgeig001.kigga.model.domain.Liga.Companion.clubExists
import com.jgeig001.kigga.model.domain.Liga.Companion.getClubBy
import com.jgeig001.kigga.model.domain.Matchday.Companion.MAX_MATCHDAYS
import com.jgeig001.kigga.model.domain.Matchday.Companion.MAX_MATCHES
import com.jgeig001.kigga.model.exceptions.ClubExistenceException
import com.jgeig001.kigga.model.exceptions.NotLoadableException
import org.json.JSONArray
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
}

class DataLoader(private var history: History) : Updater {

    /**
     * loads new available data
     */
    override fun updateData() {
        // --- LOGIC: what to load --- //
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
        var year_i = lastLoadedSeasonYear
        var loadedSeason: Season?

        // ----------------- load every whole season after curYear -----------------
        if (lastLoadedSeasonYear < this.getCurYear() || emptyHistory) {
            while (true) {
                try {
                    loadedSeason = getSeason(year_i)
                    history.addSeason(loadedSeason!!)
                } catch (e: NotLoadableException) {
                    // no new data to load
                    break
                }
                year_i += 1
            }
        } // else { data is up to date }

        // ----------------- load new match results -----------------
        // last matchday with loaded results
        val tup = history.getFirstMatchdayWithResults()
        var matchday_i: Int
        if (tup != null) {
            val firstSeasonWithMissingResults = tup.component1()
            val lastLoadedMatchday = tup.component2()

            // last matchday in numbers
            matchday_i = firstSeasonWithMissingResults.getMatchdayIndexOf(lastLoadedMatchday)
            year_i = firstSeasonWithMissingResults.getYear()
        } else {
            // no results loaded yet: load all results
            matchday_i = 1
            year_i = this.getCurYear()
        }
        // load new results into map
        var loadedResults = mutableMapOf<Int, MatchResult>()
        while (true) {
            val curSeason = history.getSeasonOf(year_i)
            try {
                while (matchday_i <= MAX_MATCHDAYS) {
                    // load results of one matchday(matchday_i) of season in year: year_i
                    loadedResults = loadNewResults(year_i, matchday_i)
                    if (loadedResults!!.isEmpty()) {
                        return
                    }
                    for ((matchID, matchResult) in loadedResults) {
                        for (match in curSeason!!.getAllMatches()) {
                            if (match.matchID == matchID) {
                                match.setResult(matchResult!!)
                                break
                            }
                        }
                    }
                    matchday_i += 1
                }
                year_i += 1
                matchday_i = 1
            } catch (e: NotLoadableException) {
                // next season is not available
                break
            }
        }
        // pass new results to model

        for ((key, value) in loadedResults) {
            history.getMatch(key)!!.setResult(value)
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
     * returns a HashMap with the matchID as key and the Result as value
     *
     * @param year
     * @param matchday_i
     * @return
     * @throws NotLoadableException
     */
    @Throws(NotLoadableException::class)
    private fun loadNewResults(year: Int, matchday_i: Int): MutableMap<Int, MatchResult> {
        val map = mutableMapOf<Int, MatchResult>()
        try {
            val url = String.format(
                "https://www.openligadb.de/api/getmatchdata/bl1/%d/%d",
                year,
                matchday_i
            )
            val jsonArrayOfMatches = readJsonFromUrl(url)
            for (i in 0 until jsonArrayOfMatches!!.length()) {
                val json_match = jsonArrayOfMatches.getJSONObject(i)
                var matchID: Int
                matchID = json_match.getInt("MatchID")
                var matchResult: MatchResult
                if (json_match.getBoolean("MatchIsFinished")) {
                    val jsonMatchResult = json_match.getJSONArray("MatchResults")
                    val halfTimeTeam1 = jsonMatchResult.getJSONObject(1).getInt("PointsTeam1")
                    val halfTimeTeam2 = jsonMatchResult.getJSONObject(1).getInt("PointsTeam2")
                    val fullTimeTeam1 = jsonMatchResult.getJSONObject(0).getInt("PointsTeam1")
                    val fullTimeTeam2 = jsonMatchResult.getJSONObject(0).getInt("PointsTeam2")
                    matchResult = MatchResult()
                    matchResult.setResults(
                        halfTimeTeam1,
                        halfTimeTeam2,
                        fullTimeTeam1,
                        fullTimeTeam2
                    )
                } else {
                    throw NotLoadableException("match is not finished")
                    // TODO: load halftime result or even live scores?
                }
                map[matchID] = matchResult
            }
            return map
        } catch (e: NotLoadableException) {
            // no data available
            return map
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return map
    }

    /**
     * Loads the whole season of `year`
     *
     * @param year
     * @return season object
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
            val jsonArrayOfMatches = readJsonFromUrl(url)
            if (jsonArrayOfMatches!!.isNull(0)) {
                throw NotLoadableException(String.format("season %d is not available", year))
            }

            // fill allMatches
            for (i in 0 until jsonArrayOfMatches.length()) {
                val json_match = jsonArrayOfMatches.getJSONObject(i)
                var match: Match

                // matchId...
                var matchID: Int
                matchID = json_match.getInt("MatchID")

                // Clubs...
                val homeTeam = getClubFrom(json_match.getJSONObject("Team1"))
                val awayTeam = getClubFrom(json_match.getJSONObject("Team2"))

                // kickoff...
                var kickoff: Long
                // 1
                val kickoffString = json_match.getString("MatchDateTime").replace('T', ' ')
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val date1 = formatter.parse(kickoffString)
                kickoff = date1.time
                match = Match(matchID, homeTeam!!, awayTeam!!, kickoff, MatchResult())

                // what about the result...?
                if (json_match.getBoolean("MatchIsFinished")) {
                    val jsonMatchResult = json_match.getJSONArray("MatchResults")
                    val halfTimeTeam1 = jsonMatchResult.getJSONObject(1).getInt("PointsTeam1")
                    val halfTimeTeam2 = jsonMatchResult.getJSONObject(1).getInt("PointsTeam2")
                    val fullTimeTeam1 = jsonMatchResult.getJSONObject(0).getInt("PointsTeam1")
                    val fullTimeTeam2 = jsonMatchResult.getJSONObject(0).getInt("PointsTeam2")
                    match.getMatchResult()
                        .setResults(halfTimeTeam1, halfTimeTeam2, fullTimeTeam1, fullTimeTeam2)
                } else {
                    try {
                        val jsonMatchResult = json_match.getJSONArray("MatchResults")
                        val halfTimeTeam1 = jsonMatchResult.getJSONObject(1).getInt("PointsTeam1")
                        val halfTimeTeam2 = jsonMatchResult.getJSONObject(1).getInt("PointsTeam2")
                        match.getMatchResult().setResultFirstHalf(halfTimeTeam1, halfTimeTeam2)
                    } catch (e: JSONException) {
                        // any results are not available yet
                    }
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

    private fun getLastUpdateOf(season: Season, matchday: Matchday): Date? {
        try {
            val url = String.format(
                "https://www.openligadb.de/api/getlastchangedate/bl1/%d/%d",
                season.getYear(), season.getMatchdayIndexOf(matchday)
            )
            val `is` = URL(url).openStream()
            val rd = BufferedReader(InputStreamReader(`is`, Charset.forName("UTF-8")))
            var jsonText = readAll(rd)
            jsonText = jsonText!!.replace('T', ' ')
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
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
            getClubBy(teamObject.getString("TeamName"))
        } catch (e: ClubExistenceException) {
            val newClub = Club(teamObject.getString("TeamName"), teamObject.getString("ShortName"))
            addClub(newClub)
            newClub
        }
    }

    private fun loadNewClubs() {
        val url =
            "https://www.openligadb.de/api/getavailableteams/bl1/" + this.getCurYear().toString()
        try {
            val jsonArrayOfClubs = readJsonFromUrl(url)
            for (i in 0 until jsonArrayOfClubs!!.length()) {
                val jsonClubObj = jsonArrayOfClubs.getJSONObject(i)
                if (!clubExists(jsonClubObj.getString("TeamName"))) {
                    addClub(
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

    /**
     * helper function
     *
     * @param rd
     * @return content as String
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun readAll(rd: Reader): String {
        val sb = StringBuilder()
        var cp: Int
        while (rd.read().also { cp = it } != -1) {
            sb.append(cp.toChar())
        }
        return sb.toString()
    }

    /**
     * Gets the JSON from the url and returns it as JSONArray
     *
     * @param url
     * @return JSONArray with all data
     * @throws IOException
     * @throws JSONException
     */
    @Throws(JSONException::class, IOException::class)
    private fun readJsonFromUrl(url: String?): JSONArray? {
        val inputStream: InputStream
        return try {
            inputStream = URL(url).openStream()
            val rd = BufferedReader(InputStreamReader(inputStream, Charset.forName("UTF-8")))
            val jsonText: String = readAll(rd)
            val json = JSONArray(jsonText)
            inputStream.close()
            json
        } catch (e: IOException) {
            val s = "[{'x': 1}]"
            JSONArray(s)
        }
    }

}