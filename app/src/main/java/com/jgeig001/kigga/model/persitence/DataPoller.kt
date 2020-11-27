package com.jgeig001.kigga.model.persitence

import android.util.Log
import com.jgeig001.kigga.model.domain.History
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class DataPoller(val history: History) {

    // constants
    private val SEC = 10L

    private var lastUpdate: Long = 0L

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
                Log.d("123", "check data")
                if (newDataAvailable()) {
                    Log.d("123", "load new data")
                    dataLoader.updateData()
                }
                delay(SEC * 1000)
            }

        }
    }

    private fun newDataAvailable(): Boolean {
        val now = nowTimestamp()
        history.getLatestSeason()?.let { season ->
            val newData = dataLoader.getLastUpdateOf(
                season,
                season.getCurrentMatchday() ?: season.getFirstMatchday()
            ).before(Date(now))
            return if (newData) {
                lastUpdate = now
                true
            } else {
                false
            }
        }
        lastUpdate = now
        return true
    }

    private fun nowTimestamp(): Long {
        if (lastUpdate == 0L)
            lastUpdate = Calendar.getInstance().time.time
        return lastUpdate
    }

}