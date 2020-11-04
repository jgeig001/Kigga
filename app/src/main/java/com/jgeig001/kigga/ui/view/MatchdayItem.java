package com.jgeig001.kigga.ui.view;

import com.jgeig001.kigga.model.domain.Match;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MatchdayItem implements Serializable {

    private ArrayList<Match> matches;
    private int number;

    public MatchdayItem(ArrayList<Match> matches, int number) {
        this.matches = matches;
        this.number = number;
    }

    /**
     * returns a list filled with list representing a matchdayDay
     * e.g. [[all matches on friday], [all matches on saturday], [all matches on sunday]]
     * @return ArrayList<ArrayList<Match>>
     */
    public ArrayList<ArrayList<Match>> matchday_day_iter() {
        ArrayList<ArrayList<Match>> listOfLists = new ArrayList<>();
        listOfLists.add(new ArrayList<Match>());
        Match prev = this.matches.get(0);
        listOfLists.get(0).add(prev);
        // loop over all matches except the first(:=prev)
        List<Match> lis = this.matches.subList(1, this.matches.size());
        for (Match match : lis) {
            // compare current matchdayDay with prev
            if (match.getMatchdayDate().equals(prev.getMatchdayDate())) {
                // if they are equal just add
                listOfLists.get(listOfLists.size() - 1).add(match);
            } else {
                // create a new list representing a new matchdayDay
                ArrayList<Match> newMatchdayDay = new ArrayList<>();
                newMatchdayDay.add(match);
                listOfLists.add(newMatchdayDay);
            }
            prev = match;
        }
        return listOfLists;
    }

    public ArrayList<Match> getMatches() {
        return matches;
    }

    public void setMatches(ArrayList<Match> matches) {
        this.matches = matches;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }
}
