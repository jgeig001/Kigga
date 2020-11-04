package com.jgeig001.kigga.ui.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.viewmodel.HomeViewModel
import com.jgeig001.kigga.viewmodel.LigaViewModel
import com.jgeig001.kigga.viewmodel.SettingsViewModel
import java.lang.IllegalArgumentException

class ViewModelFactory(private val modelWrapper: ModelWrapper) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> return HomeViewModel(
                modelWrapper
            ) as T
            modelClass.isAssignableFrom(LigaViewModel::class.java) -> return LigaViewModel(
                modelWrapper, 0
            ) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> return SettingsViewModel(
                modelWrapper
            ) as T
            else -> throw IllegalArgumentException("Unknown viewmodel class")
        }
    }

}