package com.jgeig001.kigga.ui.table

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.jgeig001.kigga.R
import com.jgeig001.kigga.model.domain.Table
import kotlinx.android.synthetic.main.view_table_element.view.*

class TableAdapter(private var table: Table, private var context: Context) : BaseAdapter() {

    private val bottomLineIndexList = listOf(3, 4, 5, 14, 15)

    override fun getCount(): Int {
        return table.maxTeams()
    }

    override fun getItem(position: Int): Any {
        return table.getTeam(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = LayoutInflater.from(context).inflate(R.layout.view_table_element, parent, false)
        val tableElement = table.getTeam(position)
        view.ele_rank.text = "${position + 1}."
        view.ele_club.text = tableElement.club.shortName
        view.ele_matches.text = tableElement.matches.toString()
        view.ele_goals.text = "${tableElement.goals}:${tableElement.opponentGoals}"
        view.ele_diff.text = (tableElement.goals - tableElement.opponentGoals).toString()
        view.ele_points.text = tableElement.points.toString()
        if (position in bottomLineIndexList)
            view.table_bottom_line.visibility = View.VISIBLE
        return view
    }

}