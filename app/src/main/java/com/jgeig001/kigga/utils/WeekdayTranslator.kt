package com.jgeig001.kigga.utils

import com.jgeig001.kigga.R

object WeekdayTranslator {

    // depending on system language
    val matchdayStrings = hashMapOf(
        "Mon" to R.string.Mon, // english
        "Tue" to R.string.Tue,
        "Wed" to R.string.Wed,
        "Thu" to R.string.Thr,
        "Fri" to R.string.Fri,
        "Sat" to R.string.Sat,
        "Sun" to R.string.Sun,
        "Mo." to R.string.Mon, // DEUTSCH
        "Di." to R.string.Tue,
        "Mi." to R.string.Wed,
        "Do." to R.string.Thr,
        "Fr." to R.string.Fri,
        "Sa." to R.string.Sat,
        "So." to R.string.Sun
    )

}