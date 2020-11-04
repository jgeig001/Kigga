package com.jgeig001.kigga.ui.view

import com.jgeig001.kigga.R
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.jgeig001.kigga.MainActivity
import com.jgeig001.kigga.ui.view.fragmentPages.HomeFragment
import com.jgeig001.kigga.ui.view.fragmentPages.LigaFragment
import com.jgeig001.kigga.ui.view.fragmentPages.SettingsFragment

private val TAB_TITLES = arrayOf(
    R.string.tab_text_1,
    R.string.tab_text_2,
    R.string.tab_text_3
)

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(private val context: Context, fm: FragmentManager) :
    FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        when (position) {
            0 -> return HomeFragment.newInstance((context as MainActivity).getModel())
            1 -> return LigaFragment.newInstance((context as MainActivity).getModel())
            2 -> return SettingsFragment.newInstance((context as MainActivity).getModel())
        }
        return Fragment()
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.resources.getString(TAB_TITLES[position])
    }

    override fun getCount(): Int {
        return TAB_TITLES.size
    }
}