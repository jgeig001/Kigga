package com.jgeig001.kigga.ui.table

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.jgeig001.kigga.R
import com.jgeig001.kigga.model.domain.Club
import com.jgeig001.kigga.model.domain.Table
import kotlinx.android.synthetic.main.view_table_element.view.*


class TableAdapter(
    private var table: Table,
    private val favClub: Club?,
    private var context: Context
) : BaseAdapter() {

    override fun getCount(): Int {
        if (table.isEmpty())
            return 0
        return table.maxTeams()
    }

    override fun getItem(position: Int): Any {
        return table.getTeam(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        // TODO: feature: highlight clubs with current running matches
        val thisClub = try {
            table.getTeam(position).club
        } catch (ex: IndexOutOfBoundsException) {
            null
        }
        val layoutId =
            if (thisClub == favClub) R.layout.view_table_element_fav_club else R.layout.view_table_element
        val view = LayoutInflater.from(context).inflate(layoutId, parent, false)
        val tableElement = try {
            table.getTeam(position)
        } catch (ex: IndexOutOfBoundsException) {
            return view
        }
        view.ele_rank.text = "${position + 1}."
        view.ele_club.text = tableElement.club.shortName
        view.ele_matches.text = tableElement.matches.toString()
        try {
            view.ele_goals.text = "${tableElement.goals}:${tableElement.opponentGoals}"
        } catch (e: IllegalStateException) {
        }
        val diff = tableElement.goals - tableElement.opponentGoals
        val diffStr = if (diff < 0) diff.toString() else "+$diff"
        view.ele_diff.text = diffStr
        view.ele_points.text = tableElement.points.toString()
        // after these positions a separator line is visible
        val bottomLineIndexList = listOf(3, 4, 5, 14, 15)
        if (position in bottomLineIndexList)
            view.table_bottom_line.visibility = View.VISIBLE
        return view
    }

    fun updateView(newTable: Table) {
        this.table = newTable
        notifyDataSetChanged()
    }

}