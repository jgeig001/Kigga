package com.jgeig001.kigga.ui.more

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.utils.FavClubChooser
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.Serializable


class MoreViewModel @ViewModelInject constructor(
    private val model: ModelWrapper,
    @ApplicationContext private val context: Context,
    @Assisted private val savedStateHandle: SavedStateHandle
) : Serializable, ViewModel() {

    var favClubLiveData: MutableLiveData<String> =
        MutableLiveData(FavClubChooser.getFavClubName(context) ?: "")

    fun onOpenDisplayModeAlertDialog() {

    }

}