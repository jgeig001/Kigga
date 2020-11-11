package com.jgeig001.kigga

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.model.persitence.PersistenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var persistenceManager: PersistenceManager
    private lateinit var model: ModelWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        navView.setupWithNavController(navController)
    }

    fun getModel(): ModelWrapper {
        return this.model
    }

    override fun onStop() {
        super.onStop()
        // TODO: save model
        persistenceManager.saveData(this)
    }

}