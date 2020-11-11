package com.jgeig001.kigga.model.persitence

import android.util.Log
import com.jgeig001.kigga.model.domain.History
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DataPoller(private var history: History) {

    private val SEC = 10L

    private var dataLoader: DataLoader = DataLoader(history)

    fun poll() {
        GlobalScope.launch {
            Log.d("123", "poll")
            dataLoader.updateData()
            delay(SEC * 1000)
        }
    }

}