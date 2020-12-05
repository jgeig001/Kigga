package com.jgeig001.kigga

import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jgeig001.kigga.model.domain.LigaClass
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.model.domain.User
import com.jgeig001.kigga.model.persitence.PersistenceManager
import com.jgeig001.kigga.utils.FavClubChooser
import com.jgeig001.kigga.utils.SharedPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
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
        //if (startCounter == 0) {
        // only on first app start
        persistenceManager.favClubCallback { user: User, liga: LigaClass ->
            // open alert dialog
            this.runOnUiThread {
                if (user.hasNoFavouriteClub()) {
                    val dialog = FavClubChooser.getClubChooserDialog(this, user, liga)
                    dialog.show() // TODO: undo
                }
            }
            //}
        }
        SharedPreferencesManager.writeInt(this, APP_START_COUNTER, startCounter + 1)

        persistenceManager.internetWarningDialog { showWarning() }
    }

    fun showWarning() {
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

    override fun onPause() {
        super.onPause()
        // TODO: save model
        // UninitializedPropertyAccessException: lateinit property persistenceManager has not been initialized
        persistenceManager.saveData(this)
    }

}