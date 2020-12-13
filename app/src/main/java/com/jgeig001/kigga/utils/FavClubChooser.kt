package com.jgeig001.kigga.utils

import android.app.AlertDialog
import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.jgeig001.kigga.R
import com.jgeig001.kigga.model.domain.Club
import com.jgeig001.kigga.model.domain.LigaClass

object FavClubChooser {

    private const val FAV_CLUB_KEY = "FAV_CLUB_KEY"

    fun getClubChooserDialog(
        context: Context,
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
                setFavClub(context, "")
            } else {
                val choosenClubName = array[which]
                setFavClub(context, choosenClubName)
            }
            dialog.dismiss()
        }

        return builder.create()
    }

    fun getLiveDataClubChooserDialog(
        context: Context,
        liga: LigaClass,
        livedata: MutableLiveData<String>
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
            val choosenClubName = array[which]
            if (choosenClubName == context.getString(R.string.no_club)) {
                // user has no favourite club
                setFavClub(context, "")
            } else {
                setFavClub(context, choosenClubName)
            }
            // update UI and trigger observer
            livedata.postValue(choosenClubName)
            dialog.dismiss()
        }

        return builder.create()
    }

    fun setFavClub(context: Context, clubName: String) {
        SharedPreferencesManager.writeString(context, FAV_CLUB_KEY, clubName)
    }

    fun getFavClubName(context: Context): String? {
        return SharedPreferencesManager.getString(context, FAV_CLUB_KEY)
    }

    fun getFavClub(context: Context, liga: LigaClass): Club {
        return liga.getClubBy(getFavClubName(context) ?: "")
    }

    fun hasNoFavouriteClub(context: Context): Boolean {
        return getFavClubName(context) == SharedPreferencesManager.DEFAULT_STRING
    }

    fun hasFavouriteClub(context: Context): Boolean {
        return getFavClubName(context) != SharedPreferencesManager.DEFAULT_STRING
    }

}