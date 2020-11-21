package com.jgeig001.kigga.model.persitence

import android.util.Log
import com.jgeig001.kigga.model.domain.History
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DataPoller(val history: History) {

    private val SEC = 10L

    private var dataLoader: DataLoader = DataLoader(history)

    fun poll() {
        GlobalScope.launch {
            while (true) {
                Log.d("123", "poll")
                // TODO: check if new data available: siehe API
                dataLoader.updateData()
                if (!history.firstLoadDone)
                    history.firstLoadDone()
                delay(SEC * 1000)
            }
        }
    }

}