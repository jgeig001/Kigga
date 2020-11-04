package com.jgeig001.kigga.viewmodel

import androidx.databinding.Observable
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jgeig001.kigga.model.domain.ModelWrapper
import java.io.Serializable

/**
 * This class represents the ViewModel for the HomeTab. It holds LiveData-Variables which are bind
 * to variables in the view defined in the XML file(layout). So if the ViewModel changes the view
 * updates automatically.
 * When the ViewModel gets some input its passed to the model.
 * The viewModel is registered at the observable model and gets notified when the model changes.
 *
 * In the callback functions update the variables which starts with an underscore(_)
 * e.g.:
 *      this._username.value = "the_new_username"
 *
 * The UI will be updated automatically.
 */
class HomeViewModel(private var model: ModelWrapper) : Serializable, ViewModel() {

    @Transient
    private var _username = MutableLiveData(model.getUsername())
    @Transient
    private var _points_curSeason = MutableLiveData("${model.getPointsCurSeason()}")
    @Transient
    private var _points_allSeasons = MutableLiveData("${model.getPointsAllTime()}")
    @Transient
    private var _favouriteClub = MutableLiveData("MrDummy")

    @Transient
    val username: LiveData<String> = _username
    @Transient
    val points_curSeason: LiveData<String> = _points_curSeason
    @Transient
    val points_allSeasons: LiveData<String> = _points_allSeasons

    init {
        print("init homeviewmodel")
        // do this in HomeFragment.kt ???
        // register callbacks at model
        this.model.getNotified(BR.username, this::setUsernameCallback)
        // more to come...
        this._username.value = this.model.getUsername()
        println(this)
        print("this._username.value:" + this._username.value)
    }

    fun setUsernameCallback(sender: Observable?, propertyId: Int) {
        val s: String = model.getUsername()
        println("$$$" + this)
        println("$$$ this._username.value" + this._username.value)
        this._username.value = s
    }

    fun setPointsCurSeason(points: Int) {
        this._points_curSeason.value = "Aktuelle Saison: ${points} Punkte"
    }

    fun onLike() {
        println("@@@" + this.model)
        this.model.getNotified(BR.username, this::setUsernameCallback)
        println("HomeViewModel.onLike()")
        this.model.setUsername(this._username.value + "#")
    }

}
