package com.jgeig001.kigga

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jgeig001.kigga.model.domain.LigaClass
import com.jgeig001.kigga.model.persitence.PersistenceManager
import com.jgeig001.kigga.utils.DisplayModeState
import com.jgeig001.kigga.utils.DisplayModeState.Companion.MANUAL_RECREATE
import com.jgeig001.kigga.utils.FavClubChooser
import com.jgeig001.kigga.utils.SharedPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val APP_START_COUNTER = "APP_START_COUNTER"
    val SPLASH_SCREEN_TIME_MS = 1500L
    val INSTANT_SPLASH_SCREEN = 0L

    @Inject
    lateinit var persistenceManager: PersistenceManager

    private lateinit var modeDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("123", "onCreate()")
        Log.d("111", "this.Activity === $this")

        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        navView.setupWithNavController(navController)

        val startCounter = SharedPreferencesManager.getInt(this, APP_START_COUNTER)

        // only on first app start
        persistenceManager.setFavClubCallback { liga: LigaClass ->
            if (startCounter == 0) {
                // open alert dialog
                GlobalScope.launch {
                    delay(6000)
                    runOnUiThread {
                        val dialog = FavClubChooser.getClubChooserDialog(this@MainActivity, liga)
                        dialog.show()
                    }
                }
            }
        }

        // inc start counter
        SharedPreferencesManager.writeInt(this, APP_START_COUNTER, startCounter + 1)

        persistenceManager.internetWarningDialog { showWarning() }

        setUpDisplayMode()

        if (SharedPreferencesManager.getBoolean(this, MANUAL_RECREATE)) {
            doNotShowSplash()
            stopSplash(INSTANT_SPLASH_SCREEN)
            SharedPreferencesManager.writeBoolean(this, MANUAL_RECREATE, false)

        } else {
            showSplash()
            stopSplash(SPLASH_SCREEN_TIME_MS)
        }
    }

    private fun setUpDisplayMode() {
        if (!listOf(MODE_NIGHT_NO, MODE_NIGHT_YES, MODE_NIGHT_FOLLOW_SYSTEM).contains(
                getDefaultNightMode()
            )
        ) {
            // app restart
            val mode: String? =
                SharedPreferencesManager.getString(
                    applicationContext,
                    DisplayModeState.DISPLAY_MODE
                )
            if (mode == SharedPreferencesManager.DEFAULT_STRING) {
                setDefaultNightMode(MODE_NIGHT_NO)
            } else {
                setDefaultNightMode(DisplayModeState.getInt(mode))
            }
        }
    }


    fun doNotShowSplash() {
        nav_view.visibility = View.VISIBLE
        splash.visibility = View.INVISIBLE
    }

    fun showSplash() {
        nav_view.visibility = View.INVISIBLE
        splash.visibility = View.VISIBLE
    }

    fun stopSplash(t: Long) {
        GlobalScope.launch {
            delay(t)
            runOnUiThread {
                val barHeight = nav_view.measuredHeight
                nav_view.visibility = View.VISIBLE
                nav_view.animate().translationYBy(barHeight.toFloat()).setDuration(0).start()
                nav_view.animate().translationYBy((-barHeight).toFloat()).setDuration(750)
                    .start()
                splash.animate().alpha(0f).setDuration(1000)
                    .setInterpolator(AccelerateInterpolator()).start()
            }
        }
    }

    private fun showWarning() {
        this.runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle(R.string.no_internet_title)
                .setMessage(R.string.no_internet_message)
                .setOnCancelListener { }
                .setNeutralButton("ok") { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("localdb", "onStop()")
        GlobalScope.launch { persistenceManager.dumpDatabase() }
    }

    public override fun onDestroy() {
        super.onDestroy()
        // prevent WindowLeak
        if (::modeDialog.isInitialized) {
            modeDialog.dismiss()
        }
    }

    fun onOpenDisplayModeAlertDialog(view: View) {
        val curPos = when (getDefaultNightMode()) {
            MODE_NIGHT_NO -> 0
            MODE_NIGHT_YES -> 1
            MODE_NIGHT_FOLLOW_SYSTEM -> 2
            MODE_NIGHT_UNSPECIFIED -> {
                if (supportsSystemDarkMode()) {
                    2
                } else {
                    0
                }
            }
            else -> 0 // light mode as default
        }
        var items = arrayOf<CharSequence>(
            getString(R.string.mode_radio_light),
            getString(R.string.mode_radio_dark)
        )
        if (supportsSystemDarkMode()) {
            items += getString(R.string.mode_radio_system)
        }

        val builder =
            androidx.appcompat.app.AlertDialog.Builder(this).setTitle(R.string.mode_dialog_title)
                .setSingleChoiceItems(items, curPos) { dialog, i ->
                    onThemeSelection(i)
                    dialog.dismiss() // close
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
        modeDialog = builder.create()
        modeDialog.show()
    }

    fun setSplashMode(now: Int, will: Int) {
        if (now != will) {
            SharedPreferencesManager.writeBoolean(this, MANUAL_RECREATE, true)
        }
    }

    private fun onThemeSelection(position: Int) {
        Log.d("123", "onThemeSelection DO show me!")
        val defaultNightMode = getDefaultNightMode()
        when (position) {
            0 -> {
                SharedPreferencesManager.writeString(
                    this,
                    DisplayModeState.DISPLAY_MODE,
                    DisplayModeState.getString(MODE_NIGHT_NO)
                )
                setSplashMode(defaultNightMode, MODE_NIGHT_NO)
                setDefaultNightMode(MODE_NIGHT_NO)
            }
            1 -> {
                SharedPreferencesManager.writeString(
                    this,
                    DisplayModeState.DISPLAY_MODE,
                    DisplayModeState.getString(MODE_NIGHT_YES)
                )
                setSplashMode(defaultNightMode, MODE_NIGHT_YES)
                setDefaultNightMode(MODE_NIGHT_YES)
            }
            2 -> {
                SharedPreferencesManager.writeString(
                    this,
                    DisplayModeState.DISPLAY_MODE,
                    DisplayModeState.getString(MODE_NIGHT_FOLLOW_SYSTEM)
                )
                setSplashMode(defaultNightMode, MODE_NIGHT_FOLLOW_SYSTEM)
                setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
            }
            else -> {
                SharedPreferencesManager.writeString(
                    this,
                    DisplayModeState.DISPLAY_MODE,
                    DisplayModeState.getString(MODE_NIGHT_NO)
                )
                setSplashMode(defaultNightMode, MODE_NIGHT_NO)
                setDefaultNightMode(MODE_NIGHT_NO)
            }
        }
        Log.d("123", "onThemeSelection do NOT show me!")
    }

    private fun supportsSystemDarkMode(): Boolean {
        return android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.P
    }

}