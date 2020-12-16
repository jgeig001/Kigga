package com.jgeig001.kigga.model.persitence

import android.util.Log
import com.jgeig001.kigga.model.domain.History
import com.jgeig001.kigga.model.domain.LigaClass
import com.jgeig001.kigga.model.domain.Season
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.Charset
import java.util.*

class DataPoller(
    private val history: History,
    private val liga: LigaClass
) {
    // constants
    private val SEC = 10L

    private var inetConnectionWorked = false
    private var firstLoadDone: Boolean = false

    private var dataLoader: DataLoader = DataLoader(history, liga)

    private var dumpDBCallback: (() -> Unit)? = null
    private var betFragmentCallback: (() -> Unit)? = null
    private var favClubCallBack: ((liga: LigaClass) -> Unit)? = null
    private lateinit var openWarningDialogCallback: () -> Unit

    fun firstLoadDone() {
        firstLoadDone = true
        dumpDBCallback?.let { cb -> cb() }
        betFragmentCallback?.let { cb -> cb() }
        favClubCallBack?.let { callbackFun ->
            callbackFun(liga)
        }
    }

    fun addFirstLoadFinishedCallback(callback: () -> Unit) {
        //this.initCallbacks.add(callback)
    }

    fun callbackNOTset(): Boolean {
        // may change if further callbacks are added
        val dumpDBCallbackIsInit = dumpDBCallback != null
        val betFragmentCallbackIsInit = betFragmentCallback != null
        val favClubCallBackIsInit = favClubCallBack != null
        return !(dumpDBCallbackIsInit && betFragmentCallbackIsInit && favClubCallBackIsInit)
    }

    /**
     * show an alertdialog if internet connection is difficult...
     */
    fun showWarning() {
        // small info dialog for user
        while (true) { // TODO: optimize
            if (this::openWarningDialogCallback.isInitialized) {
                openWarningDialogCallback()
                return
            }
        }
    }

    fun poll() {
        GlobalScope.launch(Dispatchers.IO) {

            var lastUpdate: Long = 0L

            // check internet connection
            if (!ping()) {
                showWarning()
            }

            // load data at start
            dataLoader.loadNewClubs()
            dataLoader.updateData()
            if (!firstLoadDone) {
                while (callbackNOTset()) {
                    // wait until fragment(VIEW) set callback
                    delay(25)
                }
                firstLoadDone()
            }
            dataLoader.loadTable()
            delay(SEC * 1000)

            /* ------------------------------ LOOP ------------------------------ */
            // constantly check for updates
            while (true) {
                val tuple: Pair<Boolean, Long> = try {
                    newDataAvailable(lastUpdate)
                } catch (ex: IOException) {
                    // some trouble with internet or db connection
                    if (inetConnectionWorked)
                    // connection worked once since app start: show warning
                    // avoid showing warning multiple times
                    // is set to true if connection was used successfully
                        showWarning()
                    inetConnectionWorked = false
                    Pair(false, -1L)
                }
                lastUpdate = tuple.second
                if (tuple.first || lastUpdate == 0L) {
                    dataLoader.updateData()
                    dataLoader.loadTable()
                    inetConnectionWorked = true
                }
                delay(SEC * 1000)
            }
            /* ------------------------------ LOOP ------------------------------ */

        }
    }

    /**
     * returns a pair<Boolean, Long>
     * if new data is available
     *      the pair.first is true and pair.second is the timestamp of last update
     *      else pair.first is false and pair.second is the timestamp of last update
     */
    private fun newDataAvailable(lastUpdate: Long): Pair<Boolean, Long> {
        val latestSeason: Season? = try {
            history.getRunningSeason()
        } catch (ex: NoSuchElementException) {
            null
        }
        latestSeason?.let { season ->
            val last_db_update = dataLoader.getLastUpdateOf(
                season,
                season.getCurrentMatchday() ?: season.getFirstMatchday()
            )
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
    fun ping(): Boolean {
        var inputStream: InputStream? = null
        return try {
            val url = "https://www.openligadb.de/api/getlastchangedate/bl1/2020/1"
            val connection = URL(url).openConnection()
            connection.readTimeout = 30000 // 30 sec
            inputStream = connection.getInputStream()
            val rd = BufferedReader(InputStreamReader(inputStream, Charset.forName("UTF-8")))
            val jsonText = JSON_Reader.readAll(rd)
            inputStream.close()
            jsonText.isNotEmpty()
        } catch (ex: Exception) {
            false
        } finally {
            inputStream?.close()
        }
    }

    fun internetWarningDialog(openDialog: () -> Unit) {
        openWarningDialogCallback = openDialog
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