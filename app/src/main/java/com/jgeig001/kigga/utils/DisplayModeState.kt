package com.jgeig001.kigga.utils

import androidx.appcompat.app.AppCompatDelegate

enum class DisplayModeState(val str: String, val value: Int) {
    LIGHT("LIGHT", AppCompatDelegate.MODE_NIGHT_NO),
    DARK("DARK", AppCompatDelegate.MODE_NIGHT_YES),
    SYSTEM("SYSTEM", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

    companion object {
        fun getString(i: Int): String {
            return when (i) {
                DARK.value -> DARK.str
                SYSTEM.value -> SYSTEM.str
                else -> LIGHT.str
            }
        }

        fun getInt(s: String?): Int {
            return when (s) {
                DARK.str -> DARK.value
                SYSTEM.str -> SYSTEM.value
                else -> LIGHT.value
            }
        }

        const val DISPLAY_MODE = "DISPLAY_MODE"
        const val MANUAL_RECREATE = "MANUAL_RECREATE"
    }
}