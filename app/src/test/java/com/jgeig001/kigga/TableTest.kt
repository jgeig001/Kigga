package com.jgeig001.kigga

import com.jgeig001.kigga.model.domain.Club
import com.jgeig001.kigga.model.domain.Table
import org.junit.Test

class TableTest {

    @Test
    fun testLeagueTable() {
        val table = Table()
        val clubs = mutableListOf<Club>()
        for (i in 1..Table.MAX_TEAMS) {
            val club = Club("$i.", "$i.")
            clubs.add(club)
            table.addTeam(club, Table.MAX_TEAMS - i, 0, 0, 0, 0, 0, 0)
        }

        val fav2 = clubs[1]
        val fav10 = clubs[9]
        val fav17 = clubs[16]

        // first
        var lis = table.getTop3()
        assert(lis[0].first.club == clubs[0])
        assert(lis[0].second == 1)
        assert(lis[1].first.club == clubs[1])
        assert(lis[1].second == 2)
        assert(lis[2].first.club == clubs[2])
        assert(lis[2].second == 3)

        // last
        lis = table.getFlop3()
        assert(lis[0].first.club == clubs[15])
        assert(lis[0].second == 16)
        assert(lis[1].first.club == clubs[16])
        assert(lis[1].second == 17)
        assert(lis[2].first.club == clubs[17])
        assert(lis[2].second == 18)

        // second
        lis = table.getClubsAround(fav2)
        assert(lis[0].first.club == clubs[0])
        assert(lis[0].second == 1)
        assert(lis[1].first.club == clubs[1])
        assert(lis[1].second == 2)
        assert(lis[2].first.club == clubs[2])
        assert(lis[2].second == 3)

        // second last
        lis = table.getClubsAround(fav17)
        assert(lis[0].first.club == clubs[15])
        assert(lis[0].second == 16)
        assert(lis[1].first.club == clubs[16])
        assert(lis[1].second == 17)
        assert(lis[2].first.club == clubs[17])
        assert(lis[2].second == 18)

        // middle
        lis = table.getClubsAround(fav10)
        assert(lis[0].first.club == clubs[8])
        assert(lis[0].second == 9)
        assert(lis[1].first.club == clubs[9])
        assert(lis[1].second == 10)
        assert(lis[2].first.club == clubs[10])
        assert(lis[2].second == 11)

    }

}