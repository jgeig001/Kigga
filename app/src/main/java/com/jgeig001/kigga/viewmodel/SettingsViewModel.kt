package com.jgeig001.kigga.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jgeig001.kigga.model.domain.ModelWrapper
import java.io.Serializable

class SettingsViewModel(private val model: ModelWrapper) : Serializable, ViewModel() {

    @Transient private val _username = MutableLiveData("MrDummy_Settings")

    @Transient val username: LiveData<String> = _username

    fun onLike() {
        this._username.value = this._username.value + "#"
    }

}