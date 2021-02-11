package com.jgeig001.kigga.model.persitence

interface API {
    val URL_MATCH_ID: String
    val URL_MATCHDAY: String
    val URL_TABLE: String
    val URL_SEASON: String
    val URL_CHANGE: String
    val URL_CLUBS: String
}

class OpenLigaDB_API : API {
    override val URL_MATCH_ID = "https://www.openligadb.de/api/getmatchdata/%d"
    override val URL_MATCHDAY = "https://www.openligadb.de/api/getmatchdata/bl1/%d/%d"
    override val URL_TABLE = "https://www.openligadb.de/api/getbltable/bl1/%d"
    override val URL_SEASON = "https://www.openligadb.de/api/getmatchdata/bl1/%d"
    override val URL_CHANGE = "https://www.openligadb.de/api/getlastchangedate/bl1/%d/%d"
    override val URL_CLUBS = "https://www.openligadb.de/api/getavailableteams/bl1/%d"
}
