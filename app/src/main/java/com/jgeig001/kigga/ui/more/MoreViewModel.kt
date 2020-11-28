package com.jgeig001.kigga.ui.more

import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.jgeig001.kigga.model.domain.ModelWrapper
import java.io.Serializable

class MoreViewModel @ViewModelInject constructor(
    private val model: ModelWrapper,
    @Assisted private val savedStateHandle: SavedStateHandle
) : Serializable, ViewModel() {



}