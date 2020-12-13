package com.jgeig001.kigga.utils

import android.content.Context
import com.jgeig001.kigga.model.domain.History

object SeasonSelect {

    fun getSelectedSeasonIndex(context: Context): Int {
        return SharedPreferencesManager.getInt(context, History.SELECTED_SEASON_SP_KEY)
    }

    fun setSelectedSeasonIndex(context: Context, index: Int) {
        SharedPreferencesManager.writeInt(context, History.SELECTED_SEASON_SP_KEY, index)
    }

}