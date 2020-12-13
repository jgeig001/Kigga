package com.jgeig001.kigga

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jgeig001.kigga.model.domain.LigaClass
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.model.persitence.PersistenceManager
import com.jgeig001.kigga.utils.FavClubChooser
import com.jgeig001.kigga.utils.SharedPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val APP_START_COUNTER = "APP_START_COUNTER"

    @Inject
    lateinit var persistenceManager: PersistenceManager
    private lateinit var model: ModelWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        navView.setupWithNavController(navController)

        val startCounter = SharedPreferencesManager.getInt(this, APP_START_COUNTER)
        if (startCounter == SharedPreferencesManager.DEFAULT_INT) {
            // only on first app start
            persistenceManager.setFavClubCallback { liga: LigaClass ->
                // open alert dialog
                this.runOnUiThread {
                    val dialog = FavClubChooser.getClubChooserDialog(this, liga)
                    dialog.show()
                }
            }
        }

        // inc start counter
        SharedPreferencesManager.writeInt(this, APP_START_COUNTER, startCounter + 1)

        persistenceManager.internetWarningDialog { showWarning() }
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