package com.jgeig001.kigga.callbackDispatchers

import androidx.databinding.Observable
import java.io.Serializable
import java.util.ArrayList

class CallbackDispatcher(private val notifyMap: HashMap<Int, ArrayList<(sender: Observable?, propertyId: Int) -> Unit>>) :
    Serializable, Observable.OnPropertyChangedCallback() {

    /**
     * register a callback function which is called when a property with BR.any_id changes
     */
    fun registerCallback(BR_ID: Int, callbackFunction: (sender: Observable?, propertyId: Int) -> Unit) {
        // check if list exists
        if (BR_ID in this.notifyMap) {
            this.notifyMap[BR_ID]!!.add(callbackFunction)
        } else {
            // init list first
            var lis = ArrayList<(sender: Observable?, propertyId: Int) -> Unit>()
            lis.add(callbackFunction)
            this.notifyMap[BR_ID] = lis
        }
    }

    /**
     * look for the mapped callbackfunction(s) and call them
     */
    override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
        //
        if (propertyId in notifyMap) {
            for (callback in notifyMap[propertyId]!!) {
                callback(sender, propertyId)
            }
        }
    }

}