package com.jgeig001.kigga.model.domain

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.jgeig001.kigga.BR
import java.io.Serializable

/**
 * this class represents an row/element/club in [Table]
 */
class TableElement(
    var club: Club,
    var points: Int,
    var goals: Int,
    var opponentGoals: Int,
    var won: Int,
    var draw: Int,
    var lost: Int,
    var matches: Int // amount of played matches
) : Serializable {
    override fun toString(): String {
        return "${club}, ${matches}, ${won}, ${draw}, ${lost}, ${goals}, ${opponentGoals}, ${points}"
    }
}

class Table : BaseObservable(), Serializable {

    companion object {
        // the Bundesliga has 18 club
        val MAX_TEAMS = 18
    }

    /**
     * no guarantee for correct order
     * the first element is the leader of the table and the last element the last team
     */
    @Bindable
    var tableList: MutableList<TableElement>

    init {
        tableList = mutableListOf()
    }

    fun maxTeams(): Int {
        return MAX_TEAMS
    }

    fun getRankOf(club: Club): Int {
        tableList.forEachIndexed { index, tableElement ->
            if (tableElement.club == club)
                return index + 1
        }
        return 0
    }

    fun getTeam(n: Int): TableElement {
        return tableList[n]
    }

    fun isEmpty(): Boolean {
        return tableList.isEmpty()
    }

    fun isNotEmpty(): Boolean {
        return tableList.isNotEmpty()
    }

    /**
     * adds a team to the table
     * return true if successful
     * returns false if fails: table is already complete (18 teams)
     */
    @Deprecated("use [this.setTableList()] instead")
    fun addTeam(
        club: Club,
        points: Int,
        goals: Int,
        opponentGoals: Int,
        won: Int,
        draw: Int,
        loss: Int,
        matches: Int
    ): Boolean {
        if (tableList.size == 18)
            return false
        val team = TableElement(club, points, goals, opponentGoals, won, draw, loss, matches)
        this.tableList.add(team)
        return true
    }

    /**
     * returns true if the size of the table list equals [MAX_TEAMS]
     */
    fun isComplete(): Boolean {
        return tableList.size == MAX_TEAMS
    }

    /**
     * sets [this.tableList] to an empty list
     */
    fun clearTable() {
        this.tableList = mutableListOf()
    }

    /**
     * just printing for debugging
     */
    fun printTable() {
        println(":::Tabelle:::")
        for (ele in tableList) {
            println(ele)
        }
        println("______________________________________________________________________")
    }

    /**
     * returns true if [club] is the last team of the leauge
     */
    fun isLast(club: Club): Boolean {
        return tableList.last().club == club
    }

    /**
     * returns true if [club] is table leader
     */
    fun isLeader(club: Club): Boolean {
        return tableList.first().club == club

    }

    /**
     * get top 3 leading teams
     */
    fun getTop3(): List<Pair<TableElement, Int>> {
        val lis = mutableListOf<Pair<TableElement, Int>>()
        tableList.subList(0, 3).forEachIndexed { index, tableElement ->
            lis.add(Pair(tableElement, index + 1))
        }
        return lis
    }

    /**
     * get the last 3 teams
     */
    fun getFlop3(): List<Pair<TableElement, Int>> {
        val lis = mutableListOf<Pair<TableElement, Int>>()
        for ((tableElement, rank) in tableList.subList(MAX_TEAMS - 3, MAX_TEAMS)
            .zip(arrayOf(16, 17, 18))) {
            lis.add(Pair(tableElement, rank))
        }
        return lis
    }

    /**
     * returns a list with the club before [club] the club itself and the club after
     * e.g. if [club] is 10th then following list will be returned [9thClub, 10thClub, 11thClub]
     * there is not check if the is a club before or after. make sure there is before calling!
     */
    fun getClubsAround(club: Club): List<Pair<TableElement, Int>> {
        val rank = getRankOf(club)
        val lis = mutableListOf<Pair<TableElement, Int>>()
        for ((tableElement, r) in tableList.subList(rank - 2, rank + 1)
            .zip(arrayOf(rank - 1, rank, rank + 1))) {
            lis.add(Pair(tableElement, r))
        }
        return lis
    }

    /**
     * [tableList] will be set to [lis]
     */
    fun setNewTableList(lis: List<TableElement>) {
        this.clearTable()
        this.tableList = lis.toMutableList()
        notifyPropertyChanged(BR.tableList)
    }

}