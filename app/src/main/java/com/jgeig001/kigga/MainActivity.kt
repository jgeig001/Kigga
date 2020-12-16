package com.jgeig001.kigga

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jgeig001.kigga.model.domain.LigaClass
import com.jgeig001.kigga.model.persitence.PersistenceManager
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

    @Inject
    lateinit var persistenceManager: PersistenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        navView.setupWithNavController(navController)

        val startCounter = SharedPreferencesManager.getInt(this, APP_START_COUNTER)

        // only on first app start
        persistenceManager.setFavClubCallback { liga: LigaClass ->
            if (startCounter == SharedPreferencesManager.DEFAULT_INT) {
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

        showSplash()
        stopSplash()
    }


    fun showSplash() {
        nav_view.visibility = View.INVISIBLE
        splash.visibility = View.VISIBLE
    }

    fun stopSplash() {
        GlobalScope.launch {
            delay(SPLASH_SCREEN_TIME_MS)
            runOnUiThread {
                val barHeight = nav_view.measuredHeight
                nav_view.visibility = View.VISIBLE
                nav_view.animate().translationYBy(barHeight.toFloat()).setDuration(0).start()
                nav_view.animate().translationYBy((-barHeight).toFloat()).setDuration(750).start()
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

}