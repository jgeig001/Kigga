package com.jgeig001.kigga.ui.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jgeig001.kigga.R;
import com.jgeig001.kigga.model.domain.Bet;
import com.jgeig001.kigga.model.domain.Match;
import com.jgeig001.kigga.viewmodel.LigaViewModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class LigaAdapter extends RecyclerView.Adapter<LigaAdapter.LigaViewHolder> implements Serializable {

    /**
     * static ViewHolder
     */

    public class LigaViewHolder extends RecyclerView.ViewHolder implements Serializable {
        TextView matchday_num;

        public LigaViewHolder(@NonNull View itemView) {
            super(itemView);
            LigaAdapter.this.getItemId(0);
            this.matchday_num = itemView.findViewById(R.id.matchday_x);
        }
    }

    /**
     * LigaAdapter
     */
    private LigaViewModel viewModel;
    private HashMap<Match, Bet> bets;
    private ViewGroup parent;

    // constructor
    public LigaAdapter(LigaViewModel viewModel, HashMap<Match, Bet> bets) {
        this.viewModel = viewModel;
        this.bets = bets;
    }

    // callback
    public void refreshData(LigaViewModel viewModel, HashMap<Match, Bet> bets) {
        this.viewModel = viewModel;
        this.bets = bets;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LigaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // get holder from factory and return it
        this.parent = parent;
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.matchday_card, parent, false);
        LigaViewHolder viewHolder = new LigaViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull LigaViewHolder holder, final int position) {
        // fix: prevents it from recycling, otherwise: strange things will happen <- dynamic added views are the reason for that(okay, it's maybe a bit ugly)
        holder.itemView.setHasTransientState(true);

        // TODO: matchdayItem a bit useless, put importent funcionallity somewhere else
        MatchdayItem matchdayItem = new MatchdayItem(this.viewModel.getMatchday_list().getValue().get(position).getMatches(), position);
        ArrayList<ArrayList<Match>> iter = matchdayItem.matchday_day_iter();
        holder.matchday_num.setText(String.format("%d. Spieltag", matchdayItem.getNumber() + 1));

        // iterate of single matchday_day
        for (ArrayList<Match> matchdayDay : iter) {

            // create view for matchdayDay
            View matchday_Day_View = LayoutInflater.from(this.parent.getContext()).inflate(R.layout.view_matchday_day, this.parent, false);
            ((TextView) matchday_Day_View.findViewById(R.id.matchday_day_name)).setText(matchdayDay.get(0).getMatchdayDay());

            // loop over all matches of e.g. saturday
            for (final Match match : matchdayDay) {

                // create view element for a match
                View matchView = LayoutInflater.from(this.parent.getContext()).inflate(R.layout.view_match, this.parent, false);

                // and add data to it
                ((TextView) matchView.findViewById(R.id.home_team)).setText(match.getHome_team().getShortName());
                ((TextView) matchView.findViewById(R.id.away_team)).setText(match.getAway_team().getShortName());
                ((TextView) matchView.findViewById(R.id.result)).setText(match.getResult().getRepr());

                if (match.isFinished() || match.isRunning()) {

                    if (bets.containsKey(match)) {
                        ((TextView) matchView.findViewById(R.id.result_bet)).setText(bets.get(match).getPoints());
                    } else {
                        ((TextView) matchView.findViewById(R.id.result_bet)).setText("OVER");
                    }
                    ((Button) matchView.findViewById(R.id.home_plus)).setText("⇧");
                    matchView.findViewById(R.id.home_plus).setEnabled(false);
                    ((Button) matchView.findViewById(R.id.home_minus)).setText("⇩");
                    matchView.findViewById(R.id.home_minus).setEnabled(false);
                    ((Button) matchView.findViewById(R.id.away_plus)).setText("⇧");
                    matchView.findViewById(R.id.away_plus).setEnabled(false);
                    ((Button) matchView.findViewById(R.id.away_minus)).setText("⇩");
                    matchView.findViewById(R.id.away_minus).setEnabled(false);

                } else {

                    // button listeners
                    matchView.findViewById(R.id.home_plus).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            viewModel.addHomeGoal(match);
                        }
                    });
                    matchView.findViewById(R.id.home_minus).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            viewModel.removeHomeGoal(match);
                        }
                    });
                    matchView.findViewById(R.id.away_plus).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            viewModel.addAwayGoal(match);
                        }
                    });
                    matchView.findViewById(R.id.away_minus).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            viewModel.removeAwayGoal(match);
                        }
                    });
                    try {
                        ((TextView) matchView.findViewById(R.id.result_bet)).setText(bets.get(match).repr());
                    } catch (Exception e) {
                        ((TextView) matchView.findViewById(R.id.result_bet)).setText("TIPP");
                    }

                }

                // add matchView matchday_Day_View
                ((LinearLayout) matchday_Day_View).addView(matchView);
            }

            // add matchday_Day_View to matchday_day_layout_holder
            ((LinearLayout) holder.itemView.findViewById(R.id.matchday_day_layout_holder)).addView(matchday_Day_View);

        }

    }

    @Override
    public int getItemCount() {
        return this.viewModel.getMatchdayList().size();
    }

}
