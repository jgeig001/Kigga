package com.jgeig001.kigga.utils

import com.jgeig001.kigga.R

object ArrowFunctions {

    fun trendArrow(trend: Float): Float {
        return (-trend + 1) * 180
    }

    fun getArrowDrawable(trend: Float): Int {
        if (trend > 0.5)
            return R.drawable.ic_asset_arrow_green
        return R.drawable.ic_asset_arrow_blue
    }

}