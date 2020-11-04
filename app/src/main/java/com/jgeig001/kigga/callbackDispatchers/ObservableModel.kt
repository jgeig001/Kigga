package com.jgeig001.kigga.callbackDispatchers

import androidx.databinding.Observable
import com.jgeig001.kigga.model.domain.History
import com.jgeig001.kigga.model.domain.Liga
import com.jgeig001.kigga.model.domain.Match
import com.jgeig001.kigga.model.domain.User

interface ObservableModel {

    fun getNotified(
        BR_property_ID: Int,
        callbackFunction: (sender: Observable?, propertyId: Int) -> Unit
    )

}