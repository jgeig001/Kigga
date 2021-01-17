package com.jgeig001.kigga.ui.table

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.jgeig001.kigga.databinding.ViewTableElementBinding
import com.jgeig001.kigga.databinding.ViewTableElementFavClubBinding
import com.jgeig001.kigga.model.domain.Club
import com.jgeig001.kigga.model.domain.Table

abstract class TableRowHolder(binding: ViewDataBinding) :
    RecyclerView.ViewHolder(binding.root) {

    val bottomLineIndexList = listOf(0, 3, 4, 5, 14, 15, 17)

    abstract fun setData(table: Table, position: Int)
}

class TableRowHolderFavClub(private var binding: ViewTableElementFavClubBinding) :
    TableRowHolder(binding) {
    override fun setData(table: Table, position: Int) {
        // TODO: feature: highlight clubs with current running matches
        val tableElement = try {
            table.getTeam(position)
        } catch (ex: IndexOutOfBoundsException) {
            return
        }
        binding.eleRank.text = "${position + 1}."
        binding.eleClub.text = tableElement.club.shortName
        binding.eleMatches.text = tableElement.matches.toString()
        try {
            binding.eleGoals?.text = "${tableElement.goals}:${tableElement.opponentGoals}"
        } catch (e: IllegalStateException) {
        }
        val diff = tableElement.goals - tableElement.opponentGoals
        val diffStr = if (diff < 0) diff.toString() else "+$diff"
        binding.eleDiff.text = diffStr
        binding.elePoints.text = tableElement.points.toString()
        // after these positions a separator line is visible
        if (position in bottomLineIndexList)
            binding.tableBottomLine.visibility = View.VISIBLE
        else
            binding.tableBottomLine.visibility = View.INVISIBLE
    }
}

class TableRowHolderAnyClub(private var binding: ViewTableElementBinding) :
    TableRowHolder(binding) {
    override fun setData(table: Table, position: Int) {
        // TODO: feature: highlight clubs with current running matches
        val tableElement = try {
            table.getTeam(position)
        } catch (ex: IndexOutOfBoundsException) {
            return
        }
        binding.eleRank.text = "${position + 1}."
        binding.eleClub.text = tableElement.club.shortName
        binding.eleMatches.text = tableElement.matches.toString()
        try {
            binding.eleGoals?.text = "${tableElement.goals}:${tableElement.opponentGoals}"
        } catch (e: IllegalStateException) {
        }
        val diff = tableElement.goals - tableElement.opponentGoals
        val diffStr = if (diff < 0) diff.toString() else "+$diff"
        binding.eleDiff.text = diffStr
        binding.elePoints.text = tableElement.points.toString()
        // after these positions a separator line is visible
        if (position in bottomLineIndexList)
            binding.tableBottomLine.visibility = View.VISIBLE
        else
            binding.tableBottomLine.visibility = View.INVISIBLE
    }
}

class TableAdapter(
    private var table: Table,
    private val favClub: Club?
) : RecyclerView.Adapter<TableRowHolder>() {

    companion object {
        private const val CLUB = 0
        private const val FAV_CLUB = 1
    }

    private var parent: ViewGroup? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableRowHolder {
        this.parent = parent
        val layoutInflater = LayoutInflater.from(parent.context)
        if (viewType == FAV_CLUB) {
            val binding = ViewTableElementFavClubBinding.inflate(layoutInflater, parent, false)
            return TableRowHolderFavClub(binding)
        } else {
            val binding = ViewTableElementBinding.inflate(layoutInflater, parent, false)
            return TableRowHolderAnyClub(binding)
        }
    }

    override fun onBindViewHolder(holder: TableRowHolder, position: Int) {
        holder.setData(table, position)
    }

    override fun getItemCount(): Int {
        if (table.isEmpty())
            return 0
        return table.maxTeams()
    }

    fun updateData(newTable: Table) {
        this.table = newTable
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (table.getTeam(position).club == favClub) FAV_CLUB else CLUB
    }

}