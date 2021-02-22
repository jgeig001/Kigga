package com.jgeig001.kigga.model.persitence

import com.jgeig001.kigga.model.domain.History
import com.jgeig001.kigga.model.domain.LigaClass
import com.jgeig001.kigga.model.domain.Season
import com.jgeig001.kigga.model.exceptions.ServerConnectionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.Charset
import java.util.*

/**
 * handles the polling of new data: WHEN and WHAT?
 */
class DataPoller(
    private val history: History,
    private val liga: LigaClass
) {
    // constants
    private val POLLING_DELAY = 15L
    private val SEC = 1000L // ^= 1 second

    private var inetConnectionWorked = false

    private var dataLoader: DataLoader = DataLoader(history, liga, OpenLigaDB_API())

    private var dumpDBCallback: (() -> Unit)? = null
    private var betFragmentCallback: (() -> Unit)? = null
    private var favClubCallBack: ((liga: LigaClass) -> Unit)? = null
    private lateinit var checkInetConnectionCallback: () -> Unit

    fun firstLoadDone() {
        dumpDBCallback?.let { cb -> cb() }
        betFragmentCallback?.let { cb -> cb() }
        favClubCallBack?.let { callbackFun ->
            callbackFun(liga)
        }
    }

    fun callbacksNOTset(): Boolean {
        // may change if further callbacks are added
        val dumpDBCallbackIsInit = dumpDBCallback != null
        val betFragmentCallbackIsInit = betFragmentCallback != null
        val favClubCallBackIsInit = favClubCallBack != null
        return !(dumpDBCallbackIsInit && betFragmentCallbackIsInit && favClubCallBackIsInit)
    }

    /**
     * check internet connection trouble and show an alertdialog
     */
    private suspend fun checkInetConnection() {
        while (true) {
            if (this::checkInetConnectionCallback.isInitialized) {
                checkInetConnectionCallback()
                return
            }
            delay(25)
        }
    }

    fun poll() {
        GlobalScope.launch(Dispatchers.IO) {

            var lastUpdate = 0L

            GlobalScope.launch { checkInetConnection() }

            // load data at start
            dataLoader.loadNewClubs()
            dataLoader.updateData()

            dataLoader.updateSuspendedMatches()

            history.getRunningSeason()?.checkSuspendedMatches()
            history.getRunningSeason()?.findRescheduledMatches()

            while (callbacksNOTset()) {
                // wait until fragment(VIEW) set callback
                delay(25)
            }
            firstLoadDone()

            dataLoader.loadTable()
            delay(POLLING_DELAY * SEC)

            /* ------------------------------ LOOP ------------------------------ */
            // constantly check for updates
            while (true) {
                // is data available and last update timestamp
                var dataSourceState: Pair<Boolean, Long>
                try {
                    dataLoader.updateSuspendedMatches()
                    dataSourceState = newDataAvailable(lastUpdate)
                    inetConnectionWorked = true
                } catch (ex: ServerConnectionException) {
                    // some trouble with internet or db connection
                    if (inetConnectionWorked) {
                        // connection worked once since app start: show warning
                        // avoid showing warning multiple times
                        // is set to true if connection was used successfully
                        checkInetConnection()
                    }
                    inetConnectionWorked = false
                    dataSourceState = Pair(false, -1L) // prevent updating data
                }
                lastUpdate = dataSourceState.second
                val newData = dataSourceState.first
                if (newData || lastUpdate == 0L) {
                    dataLoader.updateData()
                    dataLoader.loadTable()
                }
                delay(POLLING_DELAY * SEC)
            }
            /* ------------------------------ LOOP ------------------------------ */

        }
    }

    /**
     * returns a pair<Boolean, Long>
     * if new data is available
     *      the pair.first is true and pair.second is the timestamp of last update
     *  else
     *      pair.first is false and pair.second is the timestamp of last update
     */
    private fun newDataAvailable(lastUpdate: Long): Pair<Boolean, Long> {
        val latestSeason: Season? = try {
            history.getRunningSeason()
        } catch (ex: NoSuchElementException) {
            null
        }
        latestSeason?.let { season ->
            val matchdayNumber =
                season.getCurrentMatchday()?.getMatchdayNumber() ?: season.getFirstMatchday()
                    .getMatchdayNumber()
            val last_db_update = dataLoader.getLastUpdateOf(season.getYear(), matchdayNumber)
            return if (last_db_update.after(Date(lastUpdate))) {
                return Pair(true, last_db_update.time)
            } else {
                Pair(false, last_db_update.time)
            }

        }
        return Pair(false, 0L)
    }

    /**
     * check if the database service is available and internet connection works
     * returns true if yes
     */
    private fun pingServer(): Boolean {
        var inputStream: InputStream? = null
        return try {
            val url = "https://www.openligadb.de/api/getlastchangedate/bl1/2020/1"
            val connection = URL(url).openConnection()
            connection.readTimeout = 30000 // 30 sec
            inputStream = connection.getInputStream()
            val rd = BufferedReader(InputStreamReader(inputStream, Charset.forName("UTF-8")))
            val jsonText = JSON_Reader.readAll(rd)
            jsonText.isNotEmpty()
        } catch (ex: Exception) {
            false
        } finally {
            inputStream?.close()
        }
    }

    fun setInternetWarningDialogCallback(openDialog: () -> Unit) {
        checkInetConnectionCallback = openDialog
    }

    fun setFavClubCallback(callback: (liga: LigaClass) -> Unit) {
        this.favClubCallBack = callback
    }

    fun addDumpDBCallback(callback: () -> Unit) {
        dumpDBCallback = callback
    }

    fun addBetFragmentCallback(callback: () -> Unit) {
        betFragmentCallback = callback
    }

}