package com.jgeig001.kigga.model.persitence

import android.util.Log
import com.jgeig001.kigga.model.domain.History
import com.jgeig001.kigga.model.domain.Season
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DataPoller(val history: History) {

    // constants
    private val SEC = 10L

    private var firstLoadDone: Boolean = false
    private var initCallbacks: MutableList<() -> Unit> = mutableListOf()

    fun firstLoadDone() {
        firstLoadDone = true
        this.initCallbacks.map { cb: () -> Unit -> cb() }
    }

    fun firstLoadFinishedCallback(callback: () -> Unit) {
        this.initCallbacks.add(callback)
    }

    fun callbackNOTset(): Boolean {
        // wait until callback from [BetFragment] and from [PersistenceManager] was set => size:2
        // may change if further callbacks are added
        return initCallbacks.size != 2
    }

    private var dataLoader: DataLoader = DataLoader(history)

    fun poll() {
        GlobalScope.launch {

            var lastUpdate: Long = 0L

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

            // constantly check for updates
            while (true) {
                Log.d("123", "check if new data available")
                val tuple = newDataAvailable(lastUpdate)
                lastUpdate = tuple.second
                if (tuple.first) {
                    Log.d("123", "load new data")
                    dataLoader.updateData()
                    dataLoader.loadTable()
                } else {
                    Log.d("123", "NO new data")
                }
                delay(SEC * 1000)
            }

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
            history.getLatestSeason()
        } catch (ex: NoSuchElementException) {
            null
        }
        latestSeason?.let { season ->
            val last_db_update = dataLoader.getLastUpdateOf(
                season,
                season.getCurrentMatchday() ?: season.getFirstMatchday()
            )
            Log.d("123", "last_db_update:$last_db_update")
            Log.d("123", "lastUpdate:${Date(lastUpdate)}")
            return if (last_db_update.after(Date(lastUpdate))) {
                return Pair(true, last_db_update.time)
            } else {
                Pair(false, last_db_update.time)
            }
        }
        return Pair(false, 0L)
    }

}