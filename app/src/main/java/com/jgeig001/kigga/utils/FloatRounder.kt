package com.jgeig001.kigga.utils

import kotlin.math.roundToInt

object FloatRounder {

    fun round2D(d: Double): Float {
        return if (d < 10) {
            ((d * 100.0).roundToInt() / 100.0).toFloat() // x.yz
        } else {
            ((d * 10.0).roundToInt() / 10.0).toFloat() // xy.z
        }
    }

}