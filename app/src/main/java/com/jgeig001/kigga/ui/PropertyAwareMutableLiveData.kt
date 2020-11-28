package com.jgeig001.kigga.ui

import androidx.databinding.BaseObservable
import androidx.databinding.Observable
import androidx.lifecycle.MutableLiveData


class PropertyAwareMutableLiveData<T : BaseObservable?>(obs: T?) : MutableLiveData<T?>(obs) {

    private val callback = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            postValue(value)
        }
    }

    override fun postValue(value: T?) {
        super.postValue(value)

        value?.addOnPropertyChangedCallback(callback)
    }

}