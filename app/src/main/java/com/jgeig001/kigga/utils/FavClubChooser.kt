package com.jgeig001.kigga.utils

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Spinner
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
        return _getLiveDataClubChooserDialog(context, liga, null)
    }


    fun getLiveDataClubChooserDialog(
        context: Context,
        liga: LigaClass,
        livedata: MutableLiveData<String>
    ): AlertDialog {
        return _getLiveDataClubChooserDialog(context, liga, livedata)
    }

    private fun _getLiveDataClubChooserDialog(
        context: Context,
        liga: LigaClass,
        livedata: MutableLiveData<String>?
    ): AlertDialog {
        var allClubs: MutableCollection<Club> = liga.getAllClubs()

        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.dialog_title_club_chooser)
        builder.setMessage(R.string.dialog_message_club_chooser)

        // ist of club names + keiner
        val clubList = allClubs.map { it.clubName }.toMutableList()
        clubList.sort()
        clubList.add(0, context.getString(R.string.no_club))

        val adapter: ArrayAdapter<String> =
            ArrayAdapter(context, android.R.layout.simple_spinner_item, clubList)
        val spinnerView = LayoutInflater.from(context).inflate(R.layout.favclub_spinner, null)
        val spinner = spinnerView.findViewById<Spinner>(R.id.favClubSpinner)
        spinner.adapter = adapter

        builder.setView(spinnerView)

        builder.setNeutralButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }

        builder.setPositiveButton(R.string.ok) { dialog, _ ->
            // update object held by livedata
            Log.d("123", "which: ${spinner.selectedItemPosition}")
            val choosenClubName = clubList[spinner.selectedItemPosition]
            if (choosenClubName == context.getString(R.string.no_club)) {
                // user has no favourite club
                setFavClub(context, "")
            } else {
                setFavClub(context, choosenClubName)
            }
            // update UI and trigger observer
            livedata?.postValue(choosenClubName)
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(context.getDrawable(R.drawable.corners_stroke))

        return dialog
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