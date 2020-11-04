package com.jgeig001.kigga.model.persitence;

import com.jgeig001.kigga.model.domain.Club;
import com.jgeig001.kigga.model.domain.History;
import com.jgeig001.kigga.model.domain.Liga;
import com.jgeig001.kigga.model.domain.Match;
import com.jgeig001.kigga.model.domain.Matchday;
import com.jgeig001.kigga.model.domain.Result;
import com.jgeig001.kigga.model.domain.Season;
import com.jgeig001.kigga.model.exceptions.ClubExistenceException;
import com.jgeig001.kigga.model.exceptions.NotLoadableException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kotlin.Pair;

public class JSONLoader {

    private History history;

    public JSONLoader(History history) {
        this.history = history;
    }

    private int getCurYear() {
        Calendar cal = Calendar.getInstance();
        final int JULI = 7;
        if (cal.get(Calendar.MONTH) < JULI) {
            // e.g. JANURARY 2020 is still season 2019
            return cal.get(Calendar.YEAR) - 1;
        } else {
            return cal.get(Calendar.YEAR);
        }
    }

    /**
     * loads new available data
     */
    public void updateData() {
        // --- LOGIC: what to load --- //
        boolean emptyHistory = false;
        Season lastLoadedSeason = this.history.getCurSeason();
        int lastLoadedSeasonYear;
        if (lastLoadedSeason == null) {
            // nothing loaded yet
            emptyHistory = true;
            lastLoadedSeasonYear = this.getCurYear();
        } else {
            lastLoadedSeasonYear = lastLoadedSeason.getYear();
        }
        int year_i = lastLoadedSeasonYear;
        Season loadedSeason;

        // ----------------- load every whole season after curYear -----------------
        if (lastLoadedSeasonYear < this.getCurYear() || emptyHistory) {
            while (true) {
                try {
                    loadedSeason = this.getSeason(year_i);
                    this.history.addSeason(loadedSeason);
                } catch (NotLoadableException e) {
                    // no new data to load
                    break;
                }
                year_i += 1;
            }
        } // else { data is up to date }

        // ----------------- load new match results -----------------
        // last matchday with loaded results
        Pair<Season, Matchday> tup = this.history.getFirstMatchdayWithResults();
        int matchday_i;
        if (tup != null) {
            Season firstSeasonWithMissingResults = tup.component1();
            Matchday lastLoadedMatchday = tup.component2();

            // last matchday in numbers
            matchday_i = firstSeasonWithMissingResults.getMatchdayIndexOf(lastLoadedMatchday);
            year_i = firstSeasonWithMissingResults.getYear();
        } else {
            // no results loaded yet: load all results
            matchday_i = 1;
            year_i = this.getCurYear();
        }
        // load new results into map
        HashMap<Integer, Result> loadedResults = null;

        while (true) {
            Season curSeason = this.history.getSeasonOf(year_i);
            try {
                while (matchday_i <= Matchday.getMAX_MATCHDAYS()) {
                    // load results of one matchday(matchday_i) of season in year: year_i
                    loadedResults = this.loadNewResults(year_i, matchday_i);
                    if (loadedResults.isEmpty()) {
                        return;
                    }
                    for (Map.Entry<Integer, Result> entry : loadedResults.entrySet()) {
                        Integer matchID = entry.getKey();
                        Result result = entry.getValue();
                        for (Match match : curSeason.getAllMatches()) {
                            if (match.getMatchID() == matchID) {
                                match.setResult(result);
                                break;
                            }
                        }
                    }

                    matchday_i += 1;
                }
                year_i += 1;
                matchday_i = 1;
            } catch (NotLoadableException e) {
                // next season is not available
                break;
            }

        }
        // pass new results to model
        for (Map.Entry<Integer, Result> entry : loadedResults.entrySet()) {
            this.history.getMatch(entry.getKey()).setResult(entry.getValue());
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
    private HashMap<Integer, Result> loadNewResults(int year, int matchday_i) throws NotLoadableException {
        HashMap<Integer, Result> map = new HashMap();
        try {
            String url = String.format("https://www.openligadb.de/api/getmatchdata/bl1/%d/%d", year, matchday_i);
            JSONArray jsonArrayOfMatches = readJsonFromUrl(url);

            for (int i = 0; i < jsonArrayOfMatches.length(); i++) {
                JSONObject json_match = jsonArrayOfMatches.getJSONObject(i);

                int matchID;
                matchID = json_match.getInt("MatchID");

                Result result;
                if (json_match.getBoolean("MatchIsFinished")) {
                    JSONArray jsonMatchResult = json_match.getJSONArray("MatchResults");
                    int halfTimeTeam1 = jsonMatchResult.getJSONObject(1).getInt("PointsTeam1");
                    int halfTimeTeam2 = jsonMatchResult.getJSONObject(1).getInt("PointsTeam2");
                    int fullTimeTeam1 = jsonMatchResult.getJSONObject(0).getInt("PointsTeam1");
                    int fullTimeTeam2 = jsonMatchResult.getJSONObject(0).getInt("PointsTeam2");
                    result = new Result();
                    result.setResults(halfTimeTeam1, halfTimeTeam2, fullTimeTeam1, fullTimeTeam2);
                } else {
                    throw new NotLoadableException("match is not finished");
                    // TODO: load halftime result or even live scores?
                }
                map.put(matchID, result);
            }
            return map;
        } catch (NotLoadableException e) {
            // no data available
            return map;
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * Loads the whole season of {@code year}
     *
     * @param year
     * @return season object
     * @throws NotLoadableException
     */
    private Season getSeason(int year) throws NotLoadableException {
        // get json as string
        String url = "https://www.openligadb.de/api/getmatchdata/bl1/" + String.valueOf(year);
        // parse json
        ArrayList<Match> allMatches = new ArrayList();
        Season season;
        try {
            JSONArray jsonArrayOfMatches = this.readJsonFromUrl(url);
            if (jsonArrayOfMatches.isNull(0)) {
                throw new NotLoadableException(String.format("season %d is not available", year));
            }

            // fill allMatches
            for (int i = 0; i < jsonArrayOfMatches.length(); i++) {
                JSONObject json_match = jsonArrayOfMatches.getJSONObject(i);
                Match match;

                // matchId...
                int matchID;
                matchID = json_match.getInt("MatchID");

                // Clubs...
                Club homeTeam = getClubFrom(json_match.getJSONObject("Team1"));
                Club awayTeam = getClubFrom(json_match.getJSONObject("Team2"));

                // kickoff...
                long kickoff;
                // 1
                String kickoffString = json_match.getString("MatchDateTime").replace('T', ' ');
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date1 = formatter.parse(kickoffString);
                kickoff = date1.getTime();

                match = new Match(matchID, homeTeam, awayTeam, kickoff, new Result());

                // what about the result...?
                if (json_match.getBoolean("MatchIsFinished")) {
                    JSONArray jsonMatchResult = json_match.getJSONArray("MatchResults");
                    int halfTimeTeam1 = jsonMatchResult.getJSONObject(1).getInt("PointsTeam1");
                    int halfTimeTeam2 = jsonMatchResult.getJSONObject(1).getInt("PointsTeam2");
                    int fullTimeTeam1 = jsonMatchResult.getJSONObject(0).getInt("PointsTeam1");
                    int fullTimeTeam2 = jsonMatchResult.getJSONObject(0).getInt("PointsTeam2");
                    match.getResult().setResults(halfTimeTeam1, halfTimeTeam2, fullTimeTeam1, fullTimeTeam2);
                } else {
                    try {
                        JSONArray jsonMatchResult = json_match.getJSONArray("MatchResults");
                        int halfTimeTeam1 = jsonMatchResult.getJSONObject(1).getInt("PointsTeam1");
                        int halfTimeTeam2 = jsonMatchResult.getJSONObject(1).getInt("PointsTeam2");
                        match.getResult().setResultFirstHalf(halfTimeTeam1, halfTimeTeam2);
                    } catch (JSONException e) {
                        // any results are not available yet
                    }
                }
                // add match
                allMatches.add(match);

            }
            // generates Season from list of all matches
            ArrayList<Matchday> all_matchdays = new ArrayList();
            ArrayList<Match> cur_matchday_matches;

            for (int i = 0; i < Matchday.getMAX_MATCHDAYS() * Matchday.getMAX_MATCHES(); i += Matchday.getMAX_MATCHES()) {
                cur_matchday_matches = new ArrayList();
                for (int j = 0; j < Matchday.getMAX_MATCHES(); j++) {
                    cur_matchday_matches.add(allMatches.get(i + j));
                }
                Matchday matchday = new Matchday(cur_matchday_matches);
                all_matchdays.add(matchday);
            }
            season = new Season(all_matchdays, year);
        } catch (JSONException | IOException | ParseException e) {
            e.printStackTrace();
            throw new NotLoadableException("loading season failed");
        }
        // finally return the season
        return season;
    }

    private Date getLastUpdateOf(Season season, Matchday matchday) {
        try {
            String url = String.format("https://www.openligadb.de/api/getlastchangedate/bl1/%d/%d",
                    season.getYear(), season.getMatchdayIndexOf(matchday));
            InputStream is = new URL(url).openStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            jsonText = jsonText.replace('T', ' ');
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return formatter.parse(jsonText);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return new Date(Long.MIN_VALUE);
    }

    private Club getClubFrom(JSONObject teamObject) throws JSONException {
        try {
            return Liga.getClubBy(teamObject.getString("TeamName"));
        } catch (ClubExistenceException e) {
            Club newClub = new Club(teamObject.getString("TeamName"), teamObject.getString("ShortName"));
            Liga.addClub(newClub);
            return newClub;
        }
    }

    private void loadNewClubs() {
        String url = "https://www.openligadb.de/api/getavailableteams/bl1/" + String.valueOf(this.getCurYear());
        try {
            JSONArray jsonArrayOfClubs = this.readJsonFromUrl(url);

            for (int i = 0; i < jsonArrayOfClubs.length(); i++) {
                JSONObject jsonClubObj = jsonArrayOfClubs.getJSONObject(i);
                if (!Liga.clubExists(jsonClubObj.getString("TeamName"))) {
                    Liga.addClub(new Club(jsonClubObj.getString("TeamName"),
                            jsonClubObj.getString("ShortName")));
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * helper function
     *
     * @param rd
     * @return content as String
     * @throws IOException
     */
    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    /**
     * Gets the JSON from the url and returns it as JSONArray
     *
     * @param url
     * @return JSONArray with all data
     * @throws IOException
     * @throws JSONException
     */
    public JSONArray readJsonFromUrl(String url) throws JSONException, IOException {
        InputStream is;
        try {
            is = new URL(url).openStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONArray json = new JSONArray(jsonText);
            is.close();
            return json;
        } catch (IOException e) {
            String s = "[{'x': 1}]";
            s = "[\n" +
                    "  {\n" +
                    "    \"MatchID\": 55336,\n" +
                    "    \"MatchDateTime\": \"2019-10-04T20:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-04T18:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"7. Spieltag\",\n" +
                    "      \"GroupOrderID\": 7,\n" +
                    "      \"GroupID\": 34206\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 54,\n" +
                    "      \"TeamName\": \"Hertha BSC\",\n" +
                    "      \"ShortName\": \"Hertha BSC\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/8/81/Hertha_BSC_Logo_2012.svg/2858px-Hertha_BSC_Logo_2012.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 185,\n" +
                    "      \"TeamName\": \"Fortuna Düsseldorf\",\n" +
                    "      \"ShortName\": \"Düsseldorf\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/9/94/Fortuna_D%C3%BCsseldorf.svg/150px-Fortuna_D%C3%BCsseldorf.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-10-04T22:25:12.137\",\n" +
                    "    \"MatchIsFinished\": true,\n" +
                    "    \"MatchResults\": [\n" +
                    "      {\n" +
                    "        \"ResultID\": 89829,\n" +
                    "        \"ResultName\": \"Endergebnis\",\n" +
                    "        \"PointsTeam1\": 3,\n" +
                    "        \"PointsTeam2\": 1,\n" +
                    "        \"ResultOrderID\": 2,\n" +
                    "        \"ResultTypeID\": 2,\n" +
                    "        \"ResultDescription\": \"Ergebnis nach Ende der offiziellen Spielzeit\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"ResultID\": 89830,\n" +
                    "        \"ResultName\": \"Halbzeit\",\n" +
                    "        \"PointsTeam1\": 2,\n" +
                    "        \"PointsTeam2\": 1,\n" +
                    "        \"ResultOrderID\": 1,\n" +
                    "        \"ResultTypeID\": 1,\n" +
                    "        \"ResultDescription\": \"Zwischenstand zur Halbzeit\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Goals\": [\n" +
                    "      {\n" +
                    "        \"GoalID\": 81031,\n" +
                    "        \"ScoreTeam1\": 0,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 32,\n" +
                    "        \"GoalGetterID\": 14925,\n" +
                    "        \"GoalGetterName\": \" Hennings\",\n" +
                    "        \"IsPenalty\": true,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81032,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 38,\n" +
                    "        \"GoalGetterID\": 11542,\n" +
                    "        \"GoalGetterName\": \"Ibisevic, Vedad\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81033,\n" +
                    "        \"ScoreTeam1\": 2,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 44,\n" +
                    "        \"GoalGetterID\": 16911,\n" +
                    "        \"GoalGetterName\": \"Dilrosun\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81034,\n" +
                    "        \"ScoreTeam1\": 3,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 62,\n" +
                    "        \"GoalGetterID\": 14616,\n" +
                    "        \"GoalGetterName\": \"Vladimir Darida\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55331,\n" +
                    "    \"MatchDateTime\": \"2019-10-05T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-05T13:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"7. Spieltag\",\n" +
                    "      \"GroupOrderID\": 7,\n" +
                    "      \"GroupID\": 34206\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 40,\n" +
                    "      \"TeamName\": \"FC Bayern\",\n" +
                    "      \"ShortName\": \"FC Bayern\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/1/1f/Logo_FC_Bayern_M%C3%BCnchen_%282002%E2%80%932017%29.svg/240px-Logo_FC_Bayern_M%C3%BCnchen_%282002%E2%80%932017%29.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 123,\n" +
                    "      \"TeamName\": \"TSG 1899 Hoffenheim\",\n" +
                    "      \"ShortName\": \"Hoffenheim\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/6/64/TSG_Logo-Standard_4c.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-10-05T17:23:35.777\",\n" +
                    "    \"MatchIsFinished\": true,\n" +
                    "    \"MatchResults\": [\n" +
                    "      {\n" +
                    "        \"ResultID\": 89842,\n" +
                    "        \"ResultName\": \"Endergebnis\",\n" +
                    "        \"PointsTeam1\": 1,\n" +
                    "        \"PointsTeam2\": 2,\n" +
                    "        \"ResultOrderID\": 2,\n" +
                    "        \"ResultTypeID\": 2,\n" +
                    "        \"ResultDescription\": \"Ergebnis nach Ende der offiziellen Spielzeit\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"ResultID\": 89843,\n" +
                    "        \"ResultName\": \"Halbzeit\",\n" +
                    "        \"PointsTeam1\": 0,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 1,\n" +
                    "        \"ResultTypeID\": 1,\n" +
                    "        \"ResultDescription\": \"Zwischenstand zur Halbzeit\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Goals\": [\n" +
                    "      {\n" +
                    "        \"GoalID\": 81057,\n" +
                    "        \"ScoreTeam1\": 0,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 24,\n" +
                    "        \"GoalGetterID\": 0,\n" +
                    "        \"GoalGetterName\": \"\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81059,\n" +
                    "        \"ScoreTeam1\": 0,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 54,\n" +
                    "        \"GoalGetterID\": 16332,\n" +
                    "        \"GoalGetterName\": \"Adamyan\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81062,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 73,\n" +
                    "        \"GoalGetterID\": 1478,\n" +
                    "        \"GoalGetterName\": \"Lewandowski\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81064,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 2,\n" +
                    "        \"MatchMinute\": 79,\n" +
                    "        \"GoalGetterID\": 16332,\n" +
                    "        \"GoalGetterName\": \"Adamyan\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55332,\n" +
                    "    \"MatchDateTime\": \"2019-10-05T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-05T13:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"7. Spieltag\",\n" +
                    "      \"GroupOrderID\": 7,\n" +
                    "      \"GroupID\": 34206\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 6,\n" +
                    "      \"TeamName\": \"Bayer Leverkusen\",\n" +
                    "      \"ShortName\": \"Leverkusen\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/de/thumb/f/f7/Bayer_Leverkusen_Logo.svg/1280px-Bayer_Leverkusen_Logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 1635,\n" +
                    "      \"TeamName\": \"RB Leipzig\",\n" +
                    "      \"ShortName\": \"RBL Leipzig\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/en/thumb/0/04/RB_Leipzig_2014_logo.svg/800px-RB_Leipzig_2014_logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-10-05T17:23:37.353\",\n" +
                    "    \"MatchIsFinished\": true,\n" +
                    "    \"MatchResults\": [\n" +
                    "      {\n" +
                    "        \"ResultID\": 89844,\n" +
                    "        \"ResultName\": \"Endergebnis\",\n" +
                    "        \"PointsTeam1\": 1,\n" +
                    "        \"PointsTeam2\": 1,\n" +
                    "        \"ResultOrderID\": 2,\n" +
                    "        \"ResultTypeID\": 2,\n" +
                    "        \"ResultDescription\": \"Ergebnis nach Ende der offiziellen Spielzeit\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"ResultID\": 89845,\n" +
                    "        \"ResultName\": \"Halbzeit\",\n" +
                    "        \"PointsTeam1\": 0,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 1,\n" +
                    "        \"ResultTypeID\": 1,\n" +
                    "        \"ResultDescription\": \"Zwischenstand zur Halbzeit\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Goals\": [\n" +
                    "      {\n" +
                    "        \"GoalID\": 81063,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 66,\n" +
                    "        \"GoalGetterID\": 5798,\n" +
                    "        \"GoalGetterName\": \"Volland\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81065,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 78,\n" +
                    "        \"GoalGetterID\": 17734,\n" +
                    "        \"GoalGetterName\": \"Nkunku, C.\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55337,\n" +
                    "    \"MatchDateTime\": \"2019-10-05T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-05T13:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"7. Spieltag\",\n" +
                    "      \"GroupOrderID\": 7,\n" +
                    "      \"GroupID\": 34206\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 112,\n" +
                    "      \"TeamName\": \"SC Freiburg\",\n" +
                    "      \"ShortName\": \"SC Freiburg\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/de/thumb/f/f1/SC-Freiburg_Logo-neu.svg/739px-SC-Freiburg_Logo-neu.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 7,\n" +
                    "      \"TeamName\": \"Borussia Dortmund\",\n" +
                    "      \"ShortName\": \"Dortmund\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/6/67/Borussia_Dortmund_logo.svg/240px-Borussia_Dortmund_logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-10-05T18:49:29.013\",\n" +
                    "    \"MatchIsFinished\": true,\n" +
                    "    \"MatchResults\": [\n" +
                    "      {\n" +
                    "        \"ResultID\": 89846,\n" +
                    "        \"ResultName\": \"Endergebnis\",\n" +
                    "        \"PointsTeam1\": 2,\n" +
                    "        \"PointsTeam2\": 2,\n" +
                    "        \"ResultOrderID\": 2,\n" +
                    "        \"ResultTypeID\": 2,\n" +
                    "        \"ResultDescription\": \"Ergebnis nach Ende der offiziellen Spielzeit\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"ResultID\": 89847,\n" +
                    "        \"ResultName\": \"Halbzeit\",\n" +
                    "        \"PointsTeam1\": 0,\n" +
                    "        \"PointsTeam2\": 1,\n" +
                    "        \"ResultOrderID\": 1,\n" +
                    "        \"ResultTypeID\": 1,\n" +
                    "        \"ResultDescription\": \"Zwischenstand zur Halbzeit\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Goals\": [\n" +
                    "      {\n" +
                    "        \"GoalID\": 81053,\n" +
                    "        \"ScoreTeam1\": 0,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 20,\n" +
                    "        \"GoalGetterID\": 15873,\n" +
                    "        \"GoalGetterName\": \"Axel Witsel\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81060,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 55,\n" +
                    "        \"GoalGetterID\": 15274,\n" +
                    "        \"GoalGetterName\": \"Waldschmidt\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81061,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 2,\n" +
                    "        \"MatchMinute\": 67,\n" +
                    "        \"GoalGetterID\": 16937,\n" +
                    "        \"GoalGetterName\": \"A.Hakimi\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81066,\n" +
                    "        \"ScoreTeam1\": 2,\n" +
                    "        \"ScoreTeam2\": 2,\n" +
                    "        \"MatchMinute\": 90,\n" +
                    "        \"GoalGetterID\": 16938,\n" +
                    "        \"GoalGetterName\": \"M.Akanji\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": true,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55339,\n" +
                    "    \"MatchDateTime\": \"2019-10-05T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-05T13:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"7. Spieltag\",\n" +
                    "      \"GroupOrderID\": 7,\n" +
                    "      \"GroupID\": 34206\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 31,\n" +
                    "      \"TeamName\": \"SC Paderborn 07\",\n" +
                    "      \"ShortName\": \"Paderborn\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/e/e3/SC_Paderborn_07_Logo.svg\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 81,\n" +
                    "      \"TeamName\": \"1. FSV Mainz 05\",\n" +
                    "      \"ShortName\": \"FSV Mainz\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/9/9e/Logo_Mainz_05.svg/1099px-Logo_Mainz_05.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-10-05T17:23:06.583\",\n" +
                    "    \"MatchIsFinished\": true,\n" +
                    "    \"MatchResults\": [\n" +
                    "      {\n" +
                    "        \"ResultID\": 89848,\n" +
                    "        \"ResultName\": \"Endergebnis\",\n" +
                    "        \"PointsTeam1\": 1,\n" +
                    "        \"PointsTeam2\": 2,\n" +
                    "        \"ResultOrderID\": 2,\n" +
                    "        \"ResultTypeID\": 2,\n" +
                    "        \"ResultDescription\": \"Ergebnis nach Ende der offiziellen Spielzeit\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"ResultID\": 89849,\n" +
                    "        \"ResultName\": \"Halbzeit\",\n" +
                    "        \"PointsTeam1\": 1,\n" +
                    "        \"PointsTeam2\": 2,\n" +
                    "        \"ResultOrderID\": 1,\n" +
                    "        \"ResultTypeID\": 1,\n" +
                    "        \"ResultDescription\": \"Zwischenstand zur Halbzeit\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Goals\": [\n" +
                    "      {\n" +
                    "        \"GoalID\": 81051,\n" +
                    "        \"ScoreTeam1\": 0,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 8,\n" +
                    "        \"GoalGetterID\": 16356,\n" +
                    "        \"GoalGetterName\": \"R. Quaison\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81052,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 14,\n" +
                    "        \"GoalGetterID\": 16048,\n" +
                    "        \"GoalGetterName\": \"Zolinski\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81058,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 2,\n" +
                    "        \"MatchMinute\": 32,\n" +
                    "        \"GoalGetterID\": 2272,\n" +
                    "        \"GoalGetterName\": \"Brosinski\",\n" +
                    "        \"IsPenalty\": true,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55338,\n" +
                    "    \"MatchDateTime\": \"2019-10-05T18:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-05T16:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"7. Spieltag\",\n" +
                    "      \"GroupOrderID\": 7,\n" +
                    "      \"GroupID\": 34206\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 9,\n" +
                    "      \"TeamName\": \"FC Schalke 04\",\n" +
                    "      \"ShortName\": \"Schalke 04\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/6/6d/FC_Schalke_04_Logo.svg\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 65,\n" +
                    "      \"TeamName\": \"1. FC Köln\",\n" +
                    "      \"ShortName\": \"1. FC Köln\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/en/thumb/5/53/FC_Cologne_logo.svg/901px-FC_Cologne_logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-10-05T20:23:36.36\",\n" +
                    "    \"MatchIsFinished\": true,\n" +
                    "    \"MatchResults\": [\n" +
                    "      {\n" +
                    "        \"ResultID\": 89850,\n" +
                    "        \"ResultName\": \"Endergebnis\",\n" +
                    "        \"PointsTeam1\": 1,\n" +
                    "        \"PointsTeam2\": 1,\n" +
                    "        \"ResultOrderID\": 2,\n" +
                    "        \"ResultTypeID\": 2,\n" +
                    "        \"ResultDescription\": \"Ergebnis nach Ende der offiziellen Spielzeit\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"ResultID\": 89851,\n" +
                    "        \"ResultName\": \"Halbzeit\",\n" +
                    "        \"PointsTeam1\": 0,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 1,\n" +
                    "        \"ResultTypeID\": 1,\n" +
                    "        \"ResultDescription\": \"Zwischenstand zur Halbzeit\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Goals\": [\n" +
                    "      {\n" +
                    "        \"GoalID\": 81090,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 72,\n" +
                    "        \"GoalGetterID\": 16455,\n" +
                    "        \"GoalGetterName\": \"Serdar\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81091,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 90,\n" +
                    "        \"GoalGetterID\": 15969,\n" +
                    "        \"GoalGetterName\": \"Hector, Jonas\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55333,\n" +
                    "    \"MatchDateTime\": \"2019-10-06T13:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-06T11:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"7. Spieltag\",\n" +
                    "      \"GroupOrderID\": 7,\n" +
                    "      \"GroupID\": 34206\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 87,\n" +
                    "      \"TeamName\": \"Borussia Mönchengladbach\",\n" +
                    "      \"ShortName\": \"Gladbach\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/8/81/Borussia_Mönchengladbach_logo.svg\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 95,\n" +
                    "      \"TeamName\": \"FC Augsburg\",\n" +
                    "      \"ShortName\": \"Augsburg\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/de/b/b5/Logo_FC_Augsburg.svg\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-10-06T15:18:57.207\",\n" +
                    "    \"MatchIsFinished\": true,\n" +
                    "    \"MatchResults\": [\n" +
                    "      {\n" +
                    "        \"ResultID\": 89864,\n" +
                    "        \"ResultName\": \"Endergebnis\",\n" +
                    "        \"PointsTeam1\": 5,\n" +
                    "        \"PointsTeam2\": 1,\n" +
                    "        \"ResultOrderID\": 2,\n" +
                    "        \"ResultTypeID\": 2,\n" +
                    "        \"ResultDescription\": \"Ergebnis nach Ende der offiziellen Spielzeit\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"ResultID\": 89865,\n" +
                    "        \"ResultName\": \"Halbzeit\",\n" +
                    "        \"PointsTeam1\": 4,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 1,\n" +
                    "        \"ResultTypeID\": 1,\n" +
                    "        \"ResultDescription\": \"Zwischenstand zur Halbzeit\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Goals\": [\n" +
                    "      {\n" +
                    "        \"GoalID\": 81092,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 2,\n" +
                    "        \"GoalGetterID\": 16274,\n" +
                    "        \"GoalGetterName\": \"Zakaria\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81093,\n" +
                    "        \"ScoreTeam1\": 2,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 8,\n" +
                    "        \"GoalGetterID\": 1439,\n" +
                    "        \"GoalGetterName\": \"Herrmann\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81094,\n" +
                    "        \"ScoreTeam1\": 3,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 13,\n" +
                    "        \"GoalGetterID\": 1439,\n" +
                    "        \"GoalGetterName\": \"Herrmann\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81101,\n" +
                    "        \"ScoreTeam1\": 4,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 39,\n" +
                    "        \"GoalGetterID\": 16907,\n" +
                    "        \"GoalGetterName\": \"A.Plea\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81104,\n" +
                    "        \"ScoreTeam1\": 4,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 81,\n" +
                    "        \"GoalGetterID\": 2204,\n" +
                    "        \"GoalGetterName\": \"Niederlechner\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81105,\n" +
                    "        \"ScoreTeam1\": 5,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 83,\n" +
                    "        \"GoalGetterID\": 14727,\n" +
                    "        \"GoalGetterName\": \"Embolo\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55334,\n" +
                    "    \"MatchDateTime\": \"2019-10-06T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-06T13:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"7. Spieltag\",\n" +
                    "      \"GroupOrderID\": 7,\n" +
                    "      \"GroupID\": 34206\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 131,\n" +
                    "      \"TeamName\": \"VfL Wolfsburg\",\n" +
                    "      \"ShortName\": \"Wolfsburg\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/Logo-VfL-Wolfsburg.svg/1024px-Logo-VfL-Wolfsburg.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 80,\n" +
                    "      \"TeamName\": \"1. FC Union Berlin\",\n" +
                    "      \"ShortName\": \"Union Berlin\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/6/68/1._FC_Union_Berlin_1966_-_1990.gif/320px-1._FC_Union_Berlin_1966_-_1990.gif\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-10-06T17:25:02.47\",\n" +
                    "    \"MatchIsFinished\": true,\n" +
                    "    \"MatchResults\": [\n" +
                    "      {\n" +
                    "        \"ResultID\": 89872,\n" +
                    "        \"ResultName\": \"Endergebnis\",\n" +
                    "        \"PointsTeam1\": 1,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 2,\n" +
                    "        \"ResultTypeID\": 2,\n" +
                    "        \"ResultDescription\": \"Ergebnis nach Ende der offiziellen Spielzeit\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"ResultID\": 89873,\n" +
                    "        \"ResultName\": \"Halbzeit\",\n" +
                    "        \"PointsTeam1\": 0,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 1,\n" +
                    "        \"ResultTypeID\": 1,\n" +
                    "        \"ResultDescription\": \"Zwischenstand zur Halbzeit\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Goals\": [\n" +
                    "      {\n" +
                    "        \"GoalID\": 81108,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 69,\n" +
                    "        \"GoalGetterID\": 16906,\n" +
                    "        \"GoalGetterName\": \"Weghorst\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55335,\n" +
                    "    \"MatchDateTime\": \"2019-10-06T18:00:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-06T16:00:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"7. Spieltag\",\n" +
                    "      \"GroupOrderID\": 7,\n" +
                    "      \"GroupID\": 34206\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 91,\n" +
                    "      \"TeamName\": \"Eintracht Frankfurt\",\n" +
                    "      \"ShortName\": \"Frankfurt\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/0/04/Eintracht_Frankfurt_Logo.svg/1024px-Eintracht_Frankfurt_Logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 134,\n" +
                    "      \"TeamName\": \"Werder Bremen\",\n" +
                    "      \"ShortName\": \"Bremen\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/b/be/SV-Werder-Bremen-Logo.svg/681px-SV-Werder-Bremen-Logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-10-06T19:53:39.807\",\n" +
                    "    \"MatchIsFinished\": true,\n" +
                    "    \"MatchResults\": [\n" +
                    "      {\n" +
                    "        \"ResultID\": 89878,\n" +
                    "        \"ResultName\": \"Endergebnis\",\n" +
                    "        \"PointsTeam1\": 2,\n" +
                    "        \"PointsTeam2\": 2,\n" +
                    "        \"ResultOrderID\": 2,\n" +
                    "        \"ResultTypeID\": 2,\n" +
                    "        \"ResultDescription\": \"Ergebnis nach Ende der offiziellen Spielzeit\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"ResultID\": 89879,\n" +
                    "        \"ResultName\": \"Halbzeit\",\n" +
                    "        \"PointsTeam1\": 0,\n" +
                    "        \"PointsTeam2\": 1,\n" +
                    "        \"ResultOrderID\": 1,\n" +
                    "        \"ResultTypeID\": 1,\n" +
                    "        \"ResultDescription\": \"Zwischenstand zur Halbzeit\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Goals\": [\n" +
                    "      {\n" +
                    "        \"GoalID\": 81115,\n" +
                    "        \"ScoreTeam1\": 0,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 28,\n" +
                    "        \"GoalGetterID\": 14769,\n" +
                    "        \"GoalGetterName\": \"Klaassen\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81116,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 55,\n" +
                    "        \"GoalGetterID\": 16082,\n" +
                    "        \"GoalGetterName\": \"Sebastian Rode\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81117,\n" +
                    "        \"ScoreTeam1\": 2,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 89,\n" +
                    "        \"GoalGetterID\": 17816,\n" +
                    "        \"GoalGetterName\": \"Andre Silva\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81118,\n" +
                    "        \"ScoreTeam1\": 2,\n" +
                    "        \"ScoreTeam2\": 2,\n" +
                    "        \"MatchMinute\": 90,\n" +
                    "        \"GoalGetterID\": 16434,\n" +
                    "        \"GoalGetterName\": \"Rashica\",\n" +
                    "        \"IsPenalty\": true,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55342,\n" +
                    "    \"MatchDateTime\": \"2019-10-18T20:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-18T18:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"8. Spieltag\",\n" +
                    "      \"GroupOrderID\": 8,\n" +
                    "      \"GroupID\": 34207\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 91,\n" +
                    "      \"TeamName\": \"Eintracht Frankfurt\",\n" +
                    "      \"ShortName\": \"Frankfurt\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/0/04/Eintracht_Frankfurt_Logo.svg/1024px-Eintracht_Frankfurt_Logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 6,\n" +
                    "      \"TeamName\": \"Bayer Leverkusen\",\n" +
                    "      \"ShortName\": \"Leverkusen\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/de/thumb/f/f7/Bayer_Leverkusen_Logo.svg/1280px-Bayer_Leverkusen_Logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-10-18T22:22:13.387\",\n" +
                    "    \"MatchIsFinished\": true,\n" +
                    "    \"MatchResults\": [\n" +
                    "      {\n" +
                    "        \"ResultID\": 89922,\n" +
                    "        \"ResultName\": \"Endergebnis\",\n" +
                    "        \"PointsTeam1\": 3,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 2,\n" +
                    "        \"ResultTypeID\": 2,\n" +
                    "        \"ResultDescription\": \"Ergebnis nach Ende der offiziellen Spielzeit\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"ResultID\": 89923,\n" +
                    "        \"ResultName\": \"Halbzeit\",\n" +
                    "        \"PointsTeam1\": 2,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 1,\n" +
                    "        \"ResultTypeID\": 1,\n" +
                    "        \"ResultDescription\": \"Zwischenstand zur Halbzeit\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Goals\": [\n" +
                    "      {\n" +
                    "        \"GoalID\": 81162,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 4,\n" +
                    "        \"GoalGetterID\": 17375,\n" +
                    "        \"GoalGetterName\": \"Paciencia\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81163,\n" +
                    "        \"ScoreTeam1\": 2,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 16,\n" +
                    "        \"GoalGetterID\": 17375,\n" +
                    "        \"GoalGetterName\": \"Paciencia\",\n" +
                    "        \"IsPenalty\": true,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81168,\n" +
                    "        \"ScoreTeam1\": 3,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 80,\n" +
                    "        \"GoalGetterID\": 14795,\n" +
                    "        \"GoalGetterName\": \"Bas Dost\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55341,\n" +
                    "    \"MatchDateTime\": \"2019-10-19T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-19T13:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"8. Spieltag\",\n" +
                    "      \"GroupOrderID\": 8,\n" +
                    "      \"GroupID\": 34207\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 1635,\n" +
                    "      \"TeamName\": \"RB Leipzig\",\n" +
                    "      \"ShortName\": \"RBL Leipzig\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/en/thumb/0/04/RB_Leipzig_2014_logo.svg/800px-RB_Leipzig_2014_logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 131,\n" +
                    "      \"TeamName\": \"VfL Wolfsburg\",\n" +
                    "      \"ShortName\": \"Wolfsburg\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/Logo-VfL-Wolfsburg.svg/1024px-Logo-VfL-Wolfsburg.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-10-19T17:24:41.24\",\n" +
                    "    \"MatchIsFinished\": true,\n" +
                    "    \"MatchResults\": [\n" +
                    "      {\n" +
                    "        \"ResultID\": 89932,\n" +
                    "        \"ResultName\": \"Endergebnis\",\n" +
                    "        \"PointsTeam1\": 1,\n" +
                    "        \"PointsTeam2\": 1,\n" +
                    "        \"ResultOrderID\": 2,\n" +
                    "        \"ResultTypeID\": 2,\n" +
                    "        \"ResultDescription\": \"Ergebnis nach Ende der offiziellen Spielzeit\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"ResultID\": 89933,\n" +
                    "        \"ResultName\": \"Halbzeit\",\n" +
                    "        \"PointsTeam1\": 0,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 1,\n" +
                    "        \"ResultTypeID\": 1,\n" +
                    "        \"ResultDescription\": \"Zwischenstand zur Halbzeit\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Goals\": [\n" +
                    "      {\n" +
                    "        \"GoalID\": 81184,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 54,\n" +
                    "        \"GoalGetterID\": 16080,\n" +
                    "        \"GoalGetterName\": \"Timo Werner\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81187,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 82,\n" +
                    "        \"GoalGetterID\": 16906,\n" +
                    "        \"GoalGetterName\": \"Weghorst\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55343,\n" +
                    "    \"MatchDateTime\": \"2019-10-19T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-19T13:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"8. Spieltag\",\n" +
                    "      \"GroupOrderID\": 8,\n" +
                    "      \"GroupID\": 34207\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 134,\n" +
                    "      \"TeamName\": \"Werder Bremen\",\n" +
                    "      \"ShortName\": \"Bremen\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/b/be/SV-Werder-Bremen-Logo.svg/681px-SV-Werder-Bremen-Logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 54,\n" +
                    "      \"TeamName\": \"Hertha BSC\",\n" +
                    "      \"ShortName\": \"Hertha BSC\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/8/81/Hertha_BSC_Logo_2012.svg/2858px-Hertha_BSC_Logo_2012.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-10-19T17:24:38.537\",\n" +
                    "    \"MatchIsFinished\": true,\n" +
                    "    \"MatchResults\": [\n" +
                    "      {\n" +
                    "        \"ResultID\": 89934,\n" +
                    "        \"ResultName\": \"Endergebnis\",\n" +
                    "        \"PointsTeam1\": 1,\n" +
                    "        \"PointsTeam2\": 1,\n" +
                    "        \"ResultOrderID\": 2,\n" +
                    "        \"ResultTypeID\": 2,\n" +
                    "        \"ResultDescription\": \"Ergebnis nach Ende der offiziellen Spielzeit\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"ResultID\": 89935,\n" +
                    "        \"ResultName\": \"Halbzeit\",\n" +
                    "        \"PointsTeam1\": 1,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 1,\n" +
                    "        \"ResultTypeID\": 1,\n" +
                    "        \"ResultDescription\": \"Zwischenstand zur Halbzeit\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Goals\": [\n" +
                    "      {\n" +
                    "        \"GoalID\": 81173,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 7,\n" +
                    "        \"GoalGetterID\": 16991,\n" +
                    "        \"GoalGetterName\": \"Sargent,J.\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81185,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 70,\n" +
                    "        \"GoalGetterID\": 16913,\n" +
                    "        \"GoalGetterName\": \"Lukebakio\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55345,\n" +
                    "    \"MatchDateTime\": \"2019-10-19T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-19T13:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"8. Spieltag\",\n" +
                    "      \"GroupOrderID\": 8,\n" +
                    "      \"GroupID\": 34207\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 185,\n" +
                    "      \"TeamName\": \"Fortuna Düsseldorf\",\n" +
                    "      \"ShortName\": \"Düsseldorf\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/9/94/Fortuna_D%C3%BCsseldorf.svg/150px-Fortuna_D%C3%BCsseldorf.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 81,\n" +
                    "      \"TeamName\": \"1. FSV Mainz 05\",\n" +
                    "      \"ShortName\": \"FSV Mainz\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/9/9e/Logo_Mainz_05.svg/1099px-Logo_Mainz_05.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-10-19T17:24:36.567\",\n" +
                    "    \"MatchIsFinished\": true,\n" +
                    "    \"MatchResults\": [\n" +
                    "      {\n" +
                    "        \"ResultID\": 89940,\n" +
                    "        \"ResultName\": \"Endergebnis\",\n" +
                    "        \"PointsTeam1\": 1,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 2,\n" +
                    "        \"ResultTypeID\": 2,\n" +
                    "        \"ResultDescription\": \"Ergebnis nach Ende der offiziellen Spielzeit\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"ResultID\": 89941,\n" +
                    "        \"ResultName\": \"Halbzeit\",\n" +
                    "        \"PointsTeam1\": 0,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 1,\n" +
                    "        \"ResultTypeID\": 1,\n" +
                    "        \"ResultDescription\": \"Zwischenstand zur Halbzeit\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Goals\": [\n" +
                    "      {\n" +
                    "        \"GoalID\": 81186,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 82,\n" +
                    "        \"GoalGetterID\": 3163,\n" +
                    "        \"GoalGetterName\": \"Rouwen Hennings\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55346,\n" +
                    "    \"MatchDateTime\": \"2019-10-19T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-19T13:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"8. Spieltag\",\n" +
                    "      \"GroupOrderID\": 8,\n" +
                    "      \"GroupID\": 34207\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 95,\n" +
                    "      \"TeamName\": \"FC Augsburg\",\n" +
                    "      \"ShortName\": \"Augsburg\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/de/b/b5/Logo_FC_Augsburg.svg\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 40,\n" +
                    "      \"TeamName\": \"FC Bayern\",\n" +
                    "      \"ShortName\": \"FC Bayern\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/1/1f/Logo_FC_Bayern_M%C3%BCnchen_%282002%E2%80%932017%29.svg/240px-Logo_FC_Bayern_M%C3%BCnchen_%282002%E2%80%932017%29.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-10-19T17:24:27.927\",\n" +
                    "    \"MatchIsFinished\": true,\n" +
                    "    \"MatchResults\": [\n" +
                    "      {\n" +
                    "        \"ResultID\": 89936,\n" +
                    "        \"ResultName\": \"Endergebnis\",\n" +
                    "        \"PointsTeam1\": 2,\n" +
                    "        \"PointsTeam2\": 2,\n" +
                    "        \"ResultOrderID\": 2,\n" +
                    "        \"ResultTypeID\": 2,\n" +
                    "        \"ResultDescription\": \"Ergebnis nach Ende der offiziellen Spielzeit\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"ResultID\": 89937,\n" +
                    "        \"ResultName\": \"Halbzeit\",\n" +
                    "        \"PointsTeam1\": 1,\n" +
                    "        \"PointsTeam2\": 1,\n" +
                    "        \"ResultOrderID\": 1,\n" +
                    "        \"ResultTypeID\": 1,\n" +
                    "        \"ResultDescription\": \"Zwischenstand zur Halbzeit\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Goals\": [\n" +
                    "      {\n" +
                    "        \"GoalID\": 81171,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 1,\n" +
                    "        \"GoalGetterID\": 15376,\n" +
                    "        \"GoalGetterName\": \"Richter, Marco\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81174,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 1,\n" +
                    "        \"MatchMinute\": 14,\n" +
                    "        \"GoalGetterID\": 14563,\n" +
                    "        \"GoalGetterName\": \"Robert Lewandowski\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81183,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 2,\n" +
                    "        \"MatchMinute\": 49,\n" +
                    "        \"GoalGetterID\": 16086,\n" +
                    "        \"GoalGetterName\": \"Serge Gnabry\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81189,\n" +
                    "        \"ScoreTeam1\": 2,\n" +
                    "        \"ScoreTeam2\": 2,\n" +
                    "        \"MatchMinute\": 92,\n" +
                    "        \"GoalGetterID\": 15652,\n" +
                    "        \"GoalGetterName\": \"Finnbogason\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": true,\n" +
                    "        \"Comment\": null\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55348,\n" +
                    "    \"MatchDateTime\": \"2019-10-19T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-19T13:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"8. Spieltag\",\n" +
                    "      \"GroupOrderID\": 8,\n" +
                    "      \"GroupID\": 34207\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 80,\n" +
                    "      \"TeamName\": \"1. FC Union Berlin\",\n" +
                    "      \"ShortName\": \"Union Berlin\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/6/68/1._FC_Union_Berlin_1966_-_1990.gif/320px-1._FC_Union_Berlin_1966_-_1990.gif\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 112,\n" +
                    "      \"TeamName\": \"SC Freiburg\",\n" +
                    "      \"ShortName\": \"SC Freiburg\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/de/thumb/f/f1/SC-Freiburg_Logo-neu.svg/739px-SC-Freiburg_Logo-neu.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-10-19T17:24:47.913\",\n" +
                    "    \"MatchIsFinished\": true,\n" +
                    "    \"MatchResults\": [\n" +
                    "      {\n" +
                    "        \"ResultID\": 89938,\n" +
                    "        \"ResultName\": \"Endergebnis\",\n" +
                    "        \"PointsTeam1\": 2,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 2,\n" +
                    "        \"ResultTypeID\": 2,\n" +
                    "        \"ResultDescription\": \"Ergebnis nach Ende der offiziellen Spielzeit\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"ResultID\": 89939,\n" +
                    "        \"ResultName\": \"Halbzeit\",\n" +
                    "        \"PointsTeam1\": 1,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 1,\n" +
                    "        \"ResultTypeID\": 1,\n" +
                    "        \"ResultDescription\": \"Zwischenstand zur Halbzeit\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Goals\": [\n" +
                    "      {\n" +
                    "        \"GoalID\": 81172,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 1,\n" +
                    "        \"GoalGetterID\": 17453,\n" +
                    "        \"GoalGetterName\": \"Marius Bülter\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81188,\n" +
                    "        \"ScoreTeam1\": 2,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 84,\n" +
                    "        \"GoalGetterID\": 17840,\n" +
                    "        \"GoalGetterName\": \"Marcus Ingvartsen\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55340,\n" +
                    "    \"MatchDateTime\": \"2019-10-19T18:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-19T16:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"8. Spieltag\",\n" +
                    "      \"GroupOrderID\": 8,\n" +
                    "      \"GroupID\": 34207\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 7,\n" +
                    "      \"TeamName\": \"Borussia Dortmund\",\n" +
                    "      \"ShortName\": \"Dortmund\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/6/67/Borussia_Dortmund_logo.svg/240px-Borussia_Dortmund_logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 87,\n" +
                    "      \"TeamName\": \"Borussia Mönchengladbach\",\n" +
                    "      \"ShortName\": \"Gladbach\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/8/81/Borussia_Mönchengladbach_logo.svg\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-10-19T20:26:53.96\",\n" +
                    "    \"MatchIsFinished\": true,\n" +
                    "    \"MatchResults\": [\n" +
                    "      {\n" +
                    "        \"ResultID\": 89954,\n" +
                    "        \"ResultName\": \"Endergebnis\",\n" +
                    "        \"PointsTeam1\": 1,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 2,\n" +
                    "        \"ResultTypeID\": 2,\n" +
                    "        \"ResultDescription\": \"Ergebnis nach Ende der offiziellen Spielzeit\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"ResultID\": 89955,\n" +
                    "        \"ResultName\": \"Halbzeit\",\n" +
                    "        \"PointsTeam1\": 0,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 1,\n" +
                    "        \"ResultTypeID\": 1,\n" +
                    "        \"ResultDescription\": \"Zwischenstand zur Halbzeit\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Goals\": [\n" +
                    "      {\n" +
                    "        \"GoalID\": 81193,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 58,\n" +
                    "        \"GoalGetterID\": 3426,\n" +
                    "        \"GoalGetterName\": \"Marco Reus\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55347,\n" +
                    "    \"MatchDateTime\": \"2019-10-20T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-20T13:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"8. Spieltag\",\n" +
                    "      \"GroupOrderID\": 8,\n" +
                    "      \"GroupID\": 34207\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 65,\n" +
                    "      \"TeamName\": \"1. FC Köln\",\n" +
                    "      \"ShortName\": \"1. FC Köln\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/en/thumb/5/53/FC_Cologne_logo.svg/901px-FC_Cologne_logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 31,\n" +
                    "      \"TeamName\": \"SC Paderborn 07\",\n" +
                    "      \"ShortName\": \"Paderborn\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/e/e3/SC_Paderborn_07_Logo.svg\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-10-20T17:23:50.883\",\n" +
                    "    \"MatchIsFinished\": true,\n" +
                    "    \"MatchResults\": [\n" +
                    "      {\n" +
                    "        \"ResultID\": 89956,\n" +
                    "        \"ResultName\": \"Endergebnis\",\n" +
                    "        \"PointsTeam1\": 3,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 2,\n" +
                    "        \"ResultTypeID\": 2,\n" +
                    "        \"ResultDescription\": \"Ergebnis nach Ende der offiziellen Spielzeit\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"ResultID\": 89957,\n" +
                    "        \"ResultName\": \"Halbzeit\",\n" +
                    "        \"PointsTeam1\": 1,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 1,\n" +
                    "        \"ResultTypeID\": 1,\n" +
                    "        \"ResultDescription\": \"Zwischenstand zur Halbzeit\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Goals\": [\n" +
                    "      {\n" +
                    "        \"GoalID\": 81195,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 9,\n" +
                    "        \"GoalGetterID\": 14471,\n" +
                    "        \"GoalGetterName\": \"Simon Terodde\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81208,\n" +
                    "        \"ScoreTeam1\": 2,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 59,\n" +
                    "        \"GoalGetterID\": 16920,\n" +
                    "        \"GoalGetterName\": \"Louis Schaub\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81209,\n" +
                    "        \"ScoreTeam1\": 3,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 85,\n" +
                    "        \"GoalGetterID\": 17841,\n" +
                    "        \"GoalGetterName\": \"Bornauw, S.\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55344,\n" +
                    "    \"MatchDateTime\": \"2019-10-20T18:00:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-20T16:00:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"8. Spieltag\",\n" +
                    "      \"GroupOrderID\": 8,\n" +
                    "      \"GroupID\": 34207\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 123,\n" +
                    "      \"TeamName\": \"TSG 1899 Hoffenheim\",\n" +
                    "      \"ShortName\": \"Hoffenheim\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/6/64/TSG_Logo-Standard_4c.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 9,\n" +
                    "      \"TeamName\": \"FC Schalke 04\",\n" +
                    "      \"ShortName\": \"Schalke 04\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/6/6d/FC_Schalke_04_Logo.svg\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-10-20T19:52:12.097\",\n" +
                    "    \"MatchIsFinished\": true,\n" +
                    "    \"MatchResults\": [\n" +
                    "      {\n" +
                    "        \"ResultID\": 89968,\n" +
                    "        \"ResultName\": \"Endergebnis\",\n" +
                    "        \"PointsTeam1\": 2,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 2,\n" +
                    "        \"ResultTypeID\": 2,\n" +
                    "        \"ResultDescription\": \"Ergebnis nach Ende der offiziellen Spielzeit\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"ResultID\": 89969,\n" +
                    "        \"ResultName\": \"Halbzeit\",\n" +
                    "        \"PointsTeam1\": 0,\n" +
                    "        \"PointsTeam2\": 0,\n" +
                    "        \"ResultOrderID\": 1,\n" +
                    "        \"ResultTypeID\": 1,\n" +
                    "        \"ResultDescription\": \"Zwischenstand zur Halbzeit\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Goals\": [\n" +
                    "      {\n" +
                    "        \"GoalID\": 81210,\n" +
                    "        \"ScoreTeam1\": 1,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 72,\n" +
                    "        \"GoalGetterID\": 16104,\n" +
                    "        \"GoalGetterName\": \"Andrej Kramaric\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"GoalID\": 81211,\n" +
                    "        \"ScoreTeam1\": 2,\n" +
                    "        \"ScoreTeam2\": 0,\n" +
                    "        \"MatchMinute\": 86,\n" +
                    "        \"GoalGetterID\": 15011,\n" +
                    "        \"GoalGetterName\": \"Bebou\",\n" +
                    "        \"IsPenalty\": false,\n" +
                    "        \"IsOwnGoal\": false,\n" +
                    "        \"IsOvertime\": false,\n" +
                    "        \"Comment\": null\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55354,\n" +
                    "    \"MatchDateTime\": \"2019-10-25T20:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-25T18:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"9. Spieltag\",\n" +
                    "      \"GroupOrderID\": 9,\n" +
                    "      \"GroupID\": 34208\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 81,\n" +
                    "      \"TeamName\": \"1. FSV Mainz 05\",\n" +
                    "      \"ShortName\": \"FSV Mainz\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/9/9e/Logo_Mainz_05.svg/1099px-Logo_Mainz_05.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 65,\n" +
                    "      \"TeamName\": \"1. FC Köln\",\n" +
                    "      \"ShortName\": \"1. FC Köln\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/en/thumb/5/53/FC_Cologne_logo.svg/901px-FC_Cologne_logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-09-04T18:28:55.517\",\n" +
                    "    \"MatchIsFinished\": false,\n" +
                    "    \"MatchResults\": [],\n" +
                    "    \"Goals\": [],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55349,\n" +
                    "    \"MatchDateTime\": \"2019-10-26T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-26T13:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"9. Spieltag\",\n" +
                    "      \"GroupOrderID\": 9,\n" +
                    "      \"GroupID\": 34208\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 40,\n" +
                    "      \"TeamName\": \"FC Bayern\",\n" +
                    "      \"ShortName\": \"FC Bayern\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/1/1f/Logo_FC_Bayern_M%C3%BCnchen_%282002%E2%80%932017%29.svg/240px-Logo_FC_Bayern_M%C3%BCnchen_%282002%E2%80%932017%29.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 80,\n" +
                    "      \"TeamName\": \"1. FC Union Berlin\",\n" +
                    "      \"ShortName\": \"Union Berlin\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/6/68/1._FC_Union_Berlin_1966_-_1990.gif/320px-1._FC_Union_Berlin_1966_-_1990.gif\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-06-28T12:31:02.527\",\n" +
                    "    \"MatchIsFinished\": false,\n" +
                    "    \"MatchResults\": [],\n" +
                    "    \"Goals\": [],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55353,\n" +
                    "    \"MatchDateTime\": \"2019-10-26T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-26T13:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"9. Spieltag\",\n" +
                    "      \"GroupOrderID\": 9,\n" +
                    "      \"GroupID\": 34208\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 54,\n" +
                    "      \"TeamName\": \"Hertha BSC\",\n" +
                    "      \"ShortName\": \"Hertha BSC\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/8/81/Hertha_BSC_Logo_2012.svg/2858px-Hertha_BSC_Logo_2012.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 123,\n" +
                    "      \"TeamName\": \"TSG 1899 Hoffenheim\",\n" +
                    "      \"ShortName\": \"Hoffenheim\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/6/64/TSG_Logo-Standard_4c.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-06-28T12:31:43.98\",\n" +
                    "    \"MatchIsFinished\": false,\n" +
                    "    \"MatchResults\": [],\n" +
                    "    \"Goals\": [],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55355,\n" +
                    "    \"MatchDateTime\": \"2019-10-26T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-26T13:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"9. Spieltag\",\n" +
                    "      \"GroupOrderID\": 9,\n" +
                    "      \"GroupID\": 34208\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 112,\n" +
                    "      \"TeamName\": \"SC Freiburg\",\n" +
                    "      \"ShortName\": \"SC Freiburg\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/de/thumb/f/f1/SC-Freiburg_Logo-neu.svg/739px-SC-Freiburg_Logo-neu.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 1635,\n" +
                    "      \"TeamName\": \"RB Leipzig\",\n" +
                    "      \"ShortName\": \"RBL Leipzig\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/en/thumb/0/04/RB_Leipzig_2014_logo.svg/800px-RB_Leipzig_2014_logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-06-28T12:32:01.447\",\n" +
                    "    \"MatchIsFinished\": false,\n" +
                    "    \"MatchResults\": [],\n" +
                    "    \"Goals\": [],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55356,\n" +
                    "    \"MatchDateTime\": \"2019-10-26T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-26T13:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"9. Spieltag\",\n" +
                    "      \"GroupOrderID\": 9,\n" +
                    "      \"GroupID\": 34208\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 9,\n" +
                    "      \"TeamName\": \"FC Schalke 04\",\n" +
                    "      \"ShortName\": \"Schalke 04\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/6/6d/FC_Schalke_04_Logo.svg\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 7,\n" +
                    "      \"TeamName\": \"Borussia Dortmund\",\n" +
                    "      \"ShortName\": \"Dortmund\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/6/67/Borussia_Dortmund_logo.svg/240px-Borussia_Dortmund_logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-06-28T12:32:10.66\",\n" +
                    "    \"MatchIsFinished\": false,\n" +
                    "    \"MatchResults\": [],\n" +
                    "    \"Goals\": [],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55357,\n" +
                    "    \"MatchDateTime\": \"2019-10-26T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-26T13:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"9. Spieltag\",\n" +
                    "      \"GroupOrderID\": 9,\n" +
                    "      \"GroupID\": 34208\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 31,\n" +
                    "      \"TeamName\": \"SC Paderborn 07\",\n" +
                    "      \"ShortName\": \"Paderborn\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/e/e3/SC_Paderborn_07_Logo.svg\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 185,\n" +
                    "      \"TeamName\": \"Fortuna Düsseldorf\",\n" +
                    "      \"ShortName\": \"Düsseldorf\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/9/94/Fortuna_D%C3%BCsseldorf.svg/150px-Fortuna_D%C3%BCsseldorf.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-06-28T12:32:24.92\",\n" +
                    "    \"MatchIsFinished\": false,\n" +
                    "    \"MatchResults\": [],\n" +
                    "    \"Goals\": [],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55350,\n" +
                    "    \"MatchDateTime\": \"2019-10-26T18:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-26T16:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"9. Spieltag\",\n" +
                    "      \"GroupOrderID\": 9,\n" +
                    "      \"GroupID\": 34208\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 6,\n" +
                    "      \"TeamName\": \"Bayer Leverkusen\",\n" +
                    "      \"ShortName\": \"Leverkusen\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/de/thumb/f/f7/Bayer_Leverkusen_Logo.svg/1280px-Bayer_Leverkusen_Logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 134,\n" +
                    "      \"TeamName\": \"Werder Bremen\",\n" +
                    "      \"ShortName\": \"Bremen\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/b/be/SV-Werder-Bremen-Logo.svg/681px-SV-Werder-Bremen-Logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-09-04T18:29:10.79\",\n" +
                    "    \"MatchIsFinished\": false,\n" +
                    "    \"MatchResults\": [],\n" +
                    "    \"Goals\": [],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55352,\n" +
                    "    \"MatchDateTime\": \"2019-10-27T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-27T14:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"9. Spieltag\",\n" +
                    "      \"GroupOrderID\": 9,\n" +
                    "      \"GroupID\": 34208\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 131,\n" +
                    "      \"TeamName\": \"VfL Wolfsburg\",\n" +
                    "      \"ShortName\": \"Wolfsburg\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/Logo-VfL-Wolfsburg.svg/1024px-Logo-VfL-Wolfsburg.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 95,\n" +
                    "      \"TeamName\": \"FC Augsburg\",\n" +
                    "      \"ShortName\": \"Augsburg\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/de/b/b5/Logo_FC_Augsburg.svg\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-09-04T18:29:28.853\",\n" +
                    "    \"MatchIsFinished\": false,\n" +
                    "    \"MatchResults\": [],\n" +
                    "    \"Goals\": [],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55351,\n" +
                    "    \"MatchDateTime\": \"2019-10-27T18:00:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-10-27T17:00:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"9. Spieltag\",\n" +
                    "      \"GroupOrderID\": 9,\n" +
                    "      \"GroupID\": 34208\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 87,\n" +
                    "      \"TeamName\": \"Borussia Mönchengladbach\",\n" +
                    "      \"ShortName\": \"Gladbach\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/8/81/Borussia_Mönchengladbach_logo.svg\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 91,\n" +
                    "      \"TeamName\": \"Eintracht Frankfurt\",\n" +
                    "      \"ShortName\": \"Frankfurt\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/0/04/Eintracht_Frankfurt_Logo.svg/1024px-Eintracht_Frankfurt_Logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-09-04T18:29:50.537\",\n" +
                    "    \"MatchIsFinished\": false,\n" +
                    "    \"MatchResults\": [],\n" +
                    "    \"Goals\": [],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55363,\n" +
                    "    \"MatchDateTime\": \"2019-11-01T20:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-11-01T19:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"10. Spieltag\",\n" +
                    "      \"GroupOrderID\": 10,\n" +
                    "      \"GroupID\": 34209\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 123,\n" +
                    "      \"TeamName\": \"TSG 1899 Hoffenheim\",\n" +
                    "      \"ShortName\": \"Hoffenheim\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/6/64/TSG_Logo-Standard_4c.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 31,\n" +
                    "      \"TeamName\": \"SC Paderborn 07\",\n" +
                    "      \"ShortName\": \"Paderborn\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/e/e3/SC_Paderborn_07_Logo.svg\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-09-04T18:30:21.347\",\n" +
                    "    \"MatchIsFinished\": false,\n" +
                    "    \"MatchResults\": [],\n" +
                    "    \"Goals\": [],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55358,\n" +
                    "    \"MatchDateTime\": \"2019-11-02T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-11-02T14:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"10. Spieltag\",\n" +
                    "      \"GroupOrderID\": 10,\n" +
                    "      \"GroupID\": 34209\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 7,\n" +
                    "      \"TeamName\": \"Borussia Dortmund\",\n" +
                    "      \"ShortName\": \"Dortmund\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/6/67/Borussia_Dortmund_logo.svg/240px-Borussia_Dortmund_logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 131,\n" +
                    "      \"TeamName\": \"VfL Wolfsburg\",\n" +
                    "      \"ShortName\": \"Wolfsburg\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/Logo-VfL-Wolfsburg.svg/1024px-Logo-VfL-Wolfsburg.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-06-28T12:33:16.653\",\n" +
                    "    \"MatchIsFinished\": false,\n" +
                    "    \"MatchResults\": [],\n" +
                    "    \"Goals\": [],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55359,\n" +
                    "    \"MatchDateTime\": \"2019-11-02T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-11-02T14:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"10. Spieltag\",\n" +
                    "      \"GroupOrderID\": 10,\n" +
                    "      \"GroupID\": 34209\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 1635,\n" +
                    "      \"TeamName\": \"RB Leipzig\",\n" +
                    "      \"ShortName\": \"RBL Leipzig\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/en/thumb/0/04/RB_Leipzig_2014_logo.svg/800px-RB_Leipzig_2014_logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 81,\n" +
                    "      \"TeamName\": \"1. FSV Mainz 05\",\n" +
                    "      \"ShortName\": \"FSV Mainz\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/9/9e/Logo_Mainz_05.svg/1099px-Logo_Mainz_05.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-06-28T12:33:26.847\",\n" +
                    "    \"MatchIsFinished\": false,\n" +
                    "    \"MatchResults\": [],\n" +
                    "    \"Goals\": [],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55360,\n" +
                    "    \"MatchDateTime\": \"2019-11-02T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-11-02T14:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"10. Spieltag\",\n" +
                    "      \"GroupOrderID\": 10,\n" +
                    "      \"GroupID\": 34209\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 6,\n" +
                    "      \"TeamName\": \"Bayer Leverkusen\",\n" +
                    "      \"ShortName\": \"Leverkusen\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/de/thumb/f/f7/Bayer_Leverkusen_Logo.svg/1280px-Bayer_Leverkusen_Logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 87,\n" +
                    "      \"TeamName\": \"Borussia Mönchengladbach\",\n" +
                    "      \"ShortName\": \"Gladbach\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/8/81/Borussia_Mönchengladbach_logo.svg\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-06-28T12:33:37.567\",\n" +
                    "    \"MatchIsFinished\": false,\n" +
                    "    \"MatchResults\": [],\n" +
                    "    \"Goals\": [],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55361,\n" +
                    "    \"MatchDateTime\": \"2019-11-02T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-11-02T14:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"10. Spieltag\",\n" +
                    "      \"GroupOrderID\": 10,\n" +
                    "      \"GroupID\": 34209\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 91,\n" +
                    "      \"TeamName\": \"Eintracht Frankfurt\",\n" +
                    "      \"ShortName\": \"Frankfurt\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/0/04/Eintracht_Frankfurt_Logo.svg/1024px-Eintracht_Frankfurt_Logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 40,\n" +
                    "      \"TeamName\": \"FC Bayern\",\n" +
                    "      \"ShortName\": \"FC Bayern\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/1/1f/Logo_FC_Bayern_M%C3%BCnchen_%282002%E2%80%932017%29.svg/240px-Logo_FC_Bayern_M%C3%BCnchen_%282002%E2%80%932017%29.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-06-28T12:33:46.593\",\n" +
                    "    \"MatchIsFinished\": false,\n" +
                    "    \"MatchResults\": [],\n" +
                    "    \"Goals\": [],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55362,\n" +
                    "    \"MatchDateTime\": \"2019-11-02T15:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-11-02T14:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"10. Spieltag\",\n" +
                    "      \"GroupOrderID\": 10,\n" +
                    "      \"GroupID\": 34209\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 134,\n" +
                    "      \"TeamName\": \"Werder Bremen\",\n" +
                    "      \"ShortName\": \"Bremen\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/b/be/SV-Werder-Bremen-Logo.svg/681px-SV-Werder-Bremen-Logo.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 112,\n" +
                    "      \"TeamName\": \"SC Freiburg\",\n" +
                    "      \"ShortName\": \"SC Freiburg\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/de/thumb/f/f1/SC-Freiburg_Logo-neu.svg/739px-SC-Freiburg_Logo-neu.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-06-28T12:33:55.863\",\n" +
                    "    \"MatchIsFinished\": false,\n" +
                    "    \"MatchResults\": [],\n" +
                    "    \"Goals\": [],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"MatchID\": 55366,\n" +
                    "    \"MatchDateTime\": \"2019-11-02T18:30:00\",\n" +
                    "    \"TimeZoneID\": \"W. Europe Standard Time\",\n" +
                    "    \"LeagueId\": 4362,\n" +
                    "    \"LeagueName\": \"1. Fußball-Bundesliga 2019/2020\",\n" +
                    "    \"MatchDateTimeUTC\": \"2019-11-02T17:30:00Z\",\n" +
                    "    \"Group\": {\n" +
                    "      \"GroupName\": \"10. Spieltag\",\n" +
                    "      \"GroupOrderID\": 10,\n" +
                    "      \"GroupID\": 34209\n" +
                    "    },\n" +
                    "    \"Team1\": {\n" +
                    "      \"TeamId\": 80,\n" +
                    "      \"TeamName\": \"1. FC Union Berlin\",\n" +
                    "      \"ShortName\": \"Union Berlin\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/6/68/1._FC_Union_Berlin_1966_-_1990.gif/320px-1._FC_Union_Berlin_1966_-_1990.gif\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"Team2\": {\n" +
                    "      \"TeamId\": 54,\n" +
                    "      \"TeamName\": \"Hertha BSC\",\n" +
                    "      \"ShortName\": \"Hertha BSC\",\n" +
                    "      \"TeamIconUrl\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/8/81/Hertha_BSC_Logo_2012.svg/2858px-Hertha_BSC_Logo_2012.svg.png\",\n" +
                    "      \"TeamGroupName\": null\n" +
                    "    },\n" +
                    "    \"LastUpdateDateTime\": \"2019-09-04T18:32:11.743\",\n" +
                    "    \"MatchIsFinished\": false,\n" +
                    "    \"MatchResults\": [],\n" +
                    "    \"Goals\": [],\n" +
                    "    \"Location\": null,\n" +
                    "    \"NumberOfViewers\": null\n" +
                    "  }\n" +
                    "\n" +
                    "]";
            String jsonText = s;
            JSONArray json = new JSONArray(jsonText);
            return json;
        }
    }

}