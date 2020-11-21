package com.jgeig001.kigga.ui.home

import androidx.databinding.Observable
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.jgeig001.kigga.model.domain.ModelWrapper

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
class HomeViewModel @ViewModelInject constructor(
    private var model: ModelWrapper,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var _username = MutableLiveData(model.getUsername())

    private var _points_curSeason = MutableLiveData("${model.getPointsCurSeason()}")

    private var _points_allSeasons = MutableLiveData("${model.getPointsAllTime()}")

    private var _favouriteClub = MutableLiveData("MrDummy")

    val username: LiveData<String> = _username

    val points_curSeason: LiveData<String> = _points_curSeason

    val points_allSeasons: LiveData<String> = _points_allSeasons

    init {
        print("init homeviewmodel")
        // do this in HomeFragment.kt ???
        // register callbacks at model
        // more to come...
        this._username.value = this.model.getUsername()
    }

    fun setUsernameCallback(sender: Observable?, propertyId: Int) {
        val s: String = model.getUsername()
        this._username.value = s
    }

    fun setPointsCurSeason(points: Int) {
        this._points_curSeason.value = "Aktuelle Saison: ${points} Punkte"
    }

    fun onLike() {
        println("@@@" + this.model)
        println("HomeViewModel.onLike()")
        this.model.setUsername(this._username.value + "#")
    }

}
