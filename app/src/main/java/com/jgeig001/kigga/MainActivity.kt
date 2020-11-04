package com.jgeig001.kigga

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.model.persitence.PersistenceManager
import com.jgeig001.kigga.ui.view.SectionsPagerAdapter


class MainActivity : AppCompatActivity() {

    private lateinit var persistenceManager: PersistenceManager
    private lateinit var model: ModelWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        this.persistenceManager = PersistenceManager(this)

        this.model = persistenceManager.model

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)

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