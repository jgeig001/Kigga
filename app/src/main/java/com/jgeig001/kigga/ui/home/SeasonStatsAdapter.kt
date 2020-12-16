package com.jgeig001.kigga.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.jgeig001.kigga.R
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.model.domain.Season

class SeasonStatsAdapter(
    private var listOfSeasons: List<Season>,
    private var model: ModelWrapper,
    private var context: Context
) : BaseAdapter() {

    override fun getCount(): Int {
        return listOfSeasons.size
    }

    override fun getItem(position: Int): Any {
        return listOfSeasons[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = LayoutInflater.from(context).inflate(R.layout.season_stats, parent, false)



        //val correctResult = String.format(context.getString(R.string.seasonStatsTemplate), ,)

        return view
    }

}