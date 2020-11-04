package com.jgeig001.kigga.model.persitence.xml_deprecated;

import com.jgeig001.kigga.model.domain.Club;
import com.jgeig001.kigga.model.domain.Liga;
import com.jgeig001.kigga.model.domain.Match;
import com.jgeig001.kigga.model.domain.Matchday;
import com.jgeig001.kigga.model.domain.Result;
import com.jgeig001.kigga.model.domain.Season;
import com.jgeig001.kigga.model.exceptions.ClubExistenceException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

@Deprecated()
public class SeasonHandler extends DefaultHandler {

    private Season loadedSeason;
    private ArrayList<Match> allMatches;
    private Match match;
    private boolean bMatch;
    private boolean bMatchID;
    private boolean bHome, bAway;
    private boolean bKickoff;
    private StringBuilder data;
    private int year;
    private int matchID;
    private Club home_team, away_team;
    private int kickoff;

    public SeasonHandler(int year) {
        this.allMatches = new ArrayList<>();
        this.bMatch = false;
    }

    @Override
    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
            throws SAXException {
        if (qName.equalsIgnoreCase("ArrayOfMatch")) {

        } else if (qName.equalsIgnoreCase("Match")) {
            this.bMatch = true;
        } else if (qName.equalsIgnoreCase("MatchID")) {
            this.bMatchID = true;
        } else if (qName.equalsIgnoreCase("Team1")) {
            this.bHome = true;
        } else if (qName.equalsIgnoreCase("Team2")) {
            this.bAway = true;
        }
        data = new StringBuilder();
    }

    @Override
    public void characters(char ch[], int start, int length)
            throws SAXException {
        data.append(new String(ch, start, length));
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (qName.equalsIgnoreCase("ArrayOfMatch")) {
            // reading finished
            this.initSeason();
        } else if (qName.equalsIgnoreCase("Match")) {
            this.bMatch = false;
            this.match = new Match(this.matchID, this.home_team, this.away_team, this.kickoff, new Result());
        } else if (qName.equalsIgnoreCase("MatchID")) {
            this.bMatchID = false;
            this.matchID = Integer.valueOf(data.toString());
        } else if (qName.equalsIgnoreCase("ShortName")) {
            if (this.bHome) {

            } else if (this.bAway) {

            }
        }
    }

    private Club getClub(String shortName) {
        try {
            return Liga.getClubBy(shortName);
        } catch (ClubExistenceException e) {
            loadClubs();
        }
        return null;
    }

    private void loadClubs() {
        // TODO: loading new clubs
    }

    private void initSeason() {
        // generates Season from list of all matches
        ArrayList<Matchday> all_matchdays = new ArrayList();
        ArrayList<Match> cur_matchday_matches;

        for (int i = 0; i < Matchday.getMAX_MATCHDAYS(); i += Matchday.getMAX_MATCHES()) {
            cur_matchday_matches = new ArrayList();
            for (int j = 0; j < Matchday.getMAX_MATCHES(); j++) {
                cur_matchday_matches.add(this.allMatches.get(i + j));
            }
            Matchday matchday = new Matchday(cur_matchday_matches);
            all_matchdays.add(matchday);
        }
        this.loadedSeason = new Season(all_matchdays, this.year);
    }

    public Season getLoadedSeason() {
        return this.loadedSeason;
    }

}
