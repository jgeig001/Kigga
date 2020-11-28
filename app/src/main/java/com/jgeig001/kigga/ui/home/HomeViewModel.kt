package com.jgeig001.kigga.ui.home

import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.jgeig001.kigga.model.domain.Club
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.model.domain.TableElement

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

    var favouriteClub = MutableLiveData(model.getUser().getFavouriteClub())

    val points_curSeason: String = model.getPointsCurSeason().toString()
    val points_allSeasons: String = model.getPointsAllTime().toString()

    fun calc3Table(): List<TableElement> {
        val table = model.getLatestSeason().getTable()
        val favClub: Club = favouriteClub.value ?: table.getTeam(0).club
        return when {
            table.isLeader(favClub) -> {
                table.getTop3()
            }
            table.isLast(favClub) -> {
                table.getFlop3()
            }
            else -> {
                table.getClubsAround(favClub)
            }
        }
    }

    fun get1st(): String {
        return ""
    }

    fun get2nd(): String {
        return ""
    }

    fun get3rd(): String {
        return ""
    }

}
