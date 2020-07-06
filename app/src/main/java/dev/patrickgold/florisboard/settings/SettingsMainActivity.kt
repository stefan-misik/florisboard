/*
 * Copyright (C) 2020 Patrick Goldinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.settings

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.SettingsActivityBinding
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.ime.core.SubtypeManager
import dev.patrickgold.florisboard.util.PackageManagerUtils

private const val FRAGMENT_TAG = "FRAGMENT_TAG"
private const val PREF_RES_ID = "PREF_RES_ID"
private const val SELECTED_ITEM_ID = "SELECTED_ITEM_ID"

class SettingsMainActivity : AppCompatActivity(),
    BottomNavigationView.OnNavigationItemSelectedListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var binding: SettingsActivityBinding
    lateinit var prefs: PrefHelper
    lateinit var subtypeManager: SubtypeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = PrefHelper(this, PreferenceManager.getDefaultSharedPreferences(this))
        prefs.initDefaultPreferences()
        subtypeManager =
            SubtypeManager(this, prefs)

        val mode = when (prefs.advanced.settingsTheme) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            "auto" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
        }
        AppCompatDelegate.setDefaultNightMode(mode)

        super.onCreate(savedInstanceState)
        binding = SettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NOTE: using findViewById() instead of view binding because the binding does not include
        //       a reference to the included layout...
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        binding.bottomNavigation.setOnNavigationItemSelectedListener(this)
        binding.bottomNavigation.selectedItemId =
            savedInstanceState?.getInt(SELECTED_ITEM_ID) ?: R.id.settings__navigation__home
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SELECTED_ITEM_ID, binding.bottomNavigation.selectedItemId)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings__navigation__home -> {
                supportActionBar?.title = String.format(
                    resources.getString(R.string.settings__home__title),
                    resources.getString(R.string.app_name)
                )
                loadFragment(HomeFragment())
                true
            }
            R.id.settings__navigation__keyboard -> {
                supportActionBar?.setTitle(R.string.settings__keyboard__title)
                loadFragment(KeyboardFragment())
                true
            }
            R.id.settings__navigation__looknfeel -> {
                supportActionBar?.setTitle(R.string.settings__looknfeel__title)
                loadFragment(LooknfeelFragment())
                true
            }
            R.id.settings__navigation__gestures -> {
                supportActionBar?.setTitle(R.string.settings__gestures__title)
                loadFragment(GesturesFragment())
                true
            }
            R.id.settings__navigation__advanced -> {
                supportActionBar?.setTitle(R.string.settings__advanced__title)
                loadFragment(AdvancedFragment())
                true
            }
            else -> false
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(binding.pageFrame.id, fragment, FRAGMENT_TAG)
        //transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.settings_main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.settings__menu_help -> {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(resources.getString(R.string.florisboard__repo_url))
                )
                startActivity(browserIntent)
                true
            }
            R.id.settings__menu_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
        if (key == PrefHelper.Advanced.SETTINGS_THEME) {
            recreate()
        }
        val fragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)
        if (fragment != null && fragment.isVisible) {
            if (fragment is LooknfeelFragment) {
                if (key == PrefHelper.Theme.NAME) {
                    // TODO: recreate() is only a lazy solution, better would be to only recreate
                    //  the keyboard view
                    recreate()
                }
            }
        }
    }

    private fun updateLauncherIconStatus() {
        // Set LauncherAlias enabled/disabled state just before destroying/pausing this activity
        if (prefs.advanced.showAppIcon) {
            PackageManagerUtils.showAppIcon(this)
        } else {
            PackageManagerUtils.hideAppIcon(this)
        }
    }

    override fun onResume() {
        prefs.shared.registerOnSharedPreferenceChangeListener(this)
        super.onResume()
    }

    override fun onPause() {
        prefs.shared.unregisterOnSharedPreferenceChangeListener(this)
        updateLauncherIconStatus()
        super.onPause()
    }

    override fun onDestroy() {
        prefs.shared.unregisterOnSharedPreferenceChangeListener(this)
        updateLauncherIconStatus()
        super.onDestroy()
    }

    class PrefFragment : PreferenceFragmentCompat() {
        companion object {
            fun createFromResource(prefResId: Int): PrefFragment {
                val args = Bundle()
                args.putInt(PREF_RES_ID, prefResId)
                val fragment = PrefFragment()
                fragment.arguments = args
                return fragment
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(arguments?.getInt(PREF_RES_ID) ?: 0, rootKey)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            listView.isFocusable = false
            listView.isNestedScrollingEnabled = false
            super.onViewCreated(view, savedInstanceState)
        }
    }
}
