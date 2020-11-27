package com.jgeig001.kigga.model.domain

import android.util.Log
import java.io.Serializable

class TableElement(
    var club: Club,
    var points: Int,
    var goals: Int,
    var opponentGoals: Int,
    var won: Int,
    var draw: Int,
    var lost: Int,
    var matches: Int
) : Serializable {
    override fun toString(): String {
        return "${club}, ${matches}, ${won}, ${draw}, ${lost}, ${goals}, ${opponentGoals}, ${points}"
    }
}

class Table : Serializable {

    val MAX_TEAMS = 18

    /**
     * no guarantee for correct order
     */
    private var tableList: MutableList<TableElement>

    init {
        Log.d("123", "init Table.class")
        tableList = mutableListOf<TableElement>()
        Log.d("123", "init Table.class done -> $tableList")
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

    /**
     * adds a team to the table
     * return true if successful
     * returns false if fails: table is already complete (18 teams)
     */
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

    fun clearTable() {
        this.tableList = mutableListOf()
    }

    fun printTable() {
        println(":::Tabelle:::")
        for (ele in tableList) {
            println(ele)
        }
        println("______________________________________________________________________")
    }

}