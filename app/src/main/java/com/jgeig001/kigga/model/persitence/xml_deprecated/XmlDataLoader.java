package com.jgeig001.kigga.model.persitence.xml_deprecated;

import com.jgeig001.kigga.model.domain.History;
import com.jgeig001.kigga.model.domain.Match;
import com.jgeig001.kigga.model.domain.Matchday;
import com.jgeig001.kigga.model.domain.Result;
import com.jgeig001.kigga.model.domain.Season;
import com.jgeig001.kigga.model.exceptions.NotLoadableException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import kotlin.Pair;

/**
 * use JSON.
 */
@Deprecated()
public class XmlDataLoader {

    private int month, year;

    private XMLReader reader;

    public XmlDataLoader() {
        // init xmlReader
        try {
            this.reader = XMLReaderFactory.createXMLReader();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        this.month = Calendar.getInstance().get(Calendar.YEAR);
        this.year = Calendar.getInstance().get(Calendar.MONTH);
    }

    public void updateData(History history) {
        // logic: what to load
        int lastLoadedSeason = history.getCurSeason().getYear();
        int year_i = lastLoadedSeason;
        Season loadedSeason;

        // load every whole season after curYear
        if (lastLoadedSeason < this.year) {
            while (true) {
                try {
                    loadedSeason = this.getSeason(year_i);
                    history.addSeason(loadedSeason);
                } catch (NotLoadableException e) {
                    break;
                }
                year_i += 1;
            }
        }

        // load new match results
        // last matchday with loaded results
        Pair<Season, Matchday> tup = history.getFirstMatchdayWithResults();
        Season firstSeasonWithMissingResults = tup.component1();
        Matchday lastLoadedMatchday = tup.component2();

        // last matchday in numbers
        int matchday_i = firstSeasonWithMissingResults.getMatchdayIndexOf(lastLoadedMatchday);
        year_i = firstSeasonWithMissingResults.getYear();

        HashMap<Integer, Result> loadedResults;

        while (true) {
            Season curSeason = history.getSeasonOf(year_i);
            while (matchday_i <= Matchday.getMAX_MATCHDAYS()) {
                try {
                    loadedResults = this.loadNewResults(lastLoadedMatchday);
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
                } catch (NotLoadableException e) {
                    // next season is not available
                    break;
                }
            }
            year_i += 1;
            matchday_i = 1;
        }

    }

    private HashMap<Integer, Result> loadNewResults(Matchday lastLoadedMatchday) throws NotLoadableException {
        // get xml as string

        // parse xml
        this.reader.setContentHandler(new ResultHandler());
        return new HashMap();
    }

    private Season getSeason(int year) throws NotLoadableException {
        // get xml as string
        String content = this.getXmlAsString("https://www.openligadb.de/api/getmatchdata/bl1/" + String.valueOf(year));
        // parse xml
        this.reader.setContentHandler(new SeasonHandler(year));
        try {
            this.reader.parse(new InputSource(new StringReader(String.valueOf(content))));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return ((SeasonHandler) this.reader.getContentHandler()).getLoadedSeason();
    }

    private String getXmlAsString(String urlString) throws NotLoadableException {
        try {
            URL url = new URL(urlString);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/xml");
            con.setConnectTimeout(15000);
            con.setReadTimeout(20000);
            int status = con.getResponseCode();
            if (status != 200) {
                throw new NotLoadableException("status: " + String.valueOf(status));
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();
            return String.valueOf(content);
        } catch (MalformedURLException e) {
            throw new NotLoadableException("");
        } catch (ProtocolException e) {
            throw new NotLoadableException("");
        } catch (IOException e) {
            throw new NotLoadableException("");
        }
    }


}
