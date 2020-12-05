package com.jgeig001.kigga.utils

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jgeig001.kigga.R
import com.jgeig001.kigga.model.domain.Club
import com.jgeig001.kigga.model.domain.LigaClass
import com.jgeig001.kigga.model.domain.User

object FavClubChooser {

    fun getClubChooserDialog(
        context: Context,
        user: User,
        liga: LigaClass
    ): AlertDialog {
        var allClubs: MutableCollection<Club> = liga.getAllClubs()

        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.dialog_title_club_chooser)
        //builder.setMessage(R.string.dialog_message_club_chooser)

        // ist of club names + keine
        val clubList = allClubs.map { it.clubName }.toMutableList()
        clubList.sort()
        clubList.add(0, context.getString(R.string.no_club))
        val array = clubList.toTypedArray()

        builder.setSingleChoiceItems(
            array,
            -1
        ) { dialog, which ->
            if (array[which] == context.getString(R.string.no_club)) {
                // user has no favourite club
                user.setFavouriteClub(null)
            } else {
                val choosenClubName = array[which]
                user.setFavouriteClub(liga.getClubBy(choosenClubName))
            }
            dialog.dismiss()
        }

        return builder.create()
    }

    fun <T> getLiveDataClubChooserDialog(
        context: Context,
        user: User,
        liga: LigaClass,
        livedata: MutableLiveData<T>
    ): AlertDialog {
        var allClubs: MutableCollection<Club> = liga.getAllClubs()

        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.dialog_title_club_chooser)
        //builder.setMessage(R.string.dialog_message_club_chooser)

        // ist of club names + keine
        val clubList = allClubs.map { it.clubName }.toMutableList()
        clubList.sort()
        clubList.add(0, context.getString(R.string.no_club))
        val array = clubList.toTypedArray()

        builder.setSingleChoiceItems(
            array,
            -1
        ) { dialog, which ->
            // update object held by livedata
            if (array[which] == context.getString(R.string.no_club)) {
                // user has no favourite club
                user.setFavouriteClub(null)
            } else {
                val choosenClubName = array[which]
                user.setFavouriteClub(liga.getClubBy(choosenClubName))
            }
            // update UI and trigger observer
            livedata.postValue(user as T)
            // close dialog
            dialog.dismiss()
        }

        return builder.create()
    }


}