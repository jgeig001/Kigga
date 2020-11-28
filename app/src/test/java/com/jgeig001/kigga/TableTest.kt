package com.jgeig001.kigga

import com.jgeig001.kigga.model.domain.Club
import com.jgeig001.kigga.model.domain.Table
import org.junit.Test

class TableTest {

    @Test
    fun testTable3() {
        val table = Table()
        val clubs = mutableListOf<Club>()
        for (i in 1..Table.MAX_TEAMS) {
            val club = Club(i.toString(), i.toString())
            clubs.add(club)
            table.addTeam(club, Table.MAX_TEAMS - i, 0, 0, 0, 0, 0, 0)
        }

        val fav2 = clubs[1]
        val fav10 = clubs[9]
        val fav17 = clubs[16]

        // first
        var lis = table.getTop3()
        assert(lis[0].club == clubs[0])
        assert(lis[1].club == clubs[1])
        assert(lis[2].club == clubs[2])

        // last
        lis = table.getFlop3()
        assert(lis[0].club == clubs[15])
        assert(lis[1].club == clubs[16])
        assert(lis[2].club == clubs[17])

        // second
        lis = table.getClubsAround(fav2)
        assert(lis[0].club == clubs[0])
        assert(lis[1].club == clubs[1])
        assert(lis[2].club == clubs[2])

        // second last
        lis = table.getClubsAround(fav17)
        assert(lis[0].club == clubs[15])
        assert(lis[1].club == clubs[16])
        assert(lis[2].club == clubs[17])

        // middle
        lis = table.getClubsAround(fav10)
        assert(lis[0].club == clubs[8])
        assert(lis[1].club == clubs[9])
        assert(lis[2].club == clubs[10])

    }

}