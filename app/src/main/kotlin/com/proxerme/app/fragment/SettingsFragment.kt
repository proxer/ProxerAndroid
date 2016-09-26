package com.proxerme.app.fragment

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.preference.TwoStatePreference
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.proxerme.app.R
import com.proxerme.app.application.MainApplication
import com.proxerme.app.dialog.HentaiConfirmationDialog
import com.proxerme.app.helper.PreferenceHelper
import com.proxerme.app.helper.ServiceHelper
import com.proxerme.app.interfaces.OnActivityListener
import com.proxerme.app.util.Utils
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class SettingsFragment : PreferenceFragmentCompat(), OnActivityListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private val LIBRARIES: Array<String> = arrayOf("glide", "jodatimeandroid", "hawk",
                "materialdialogs", "eventbus", "circleimageview", "okhttp", "leakcanary")
        private val EXCLUDED_LIBRARIES: Array<String> = arrayOf("fastadapter", "materialize")
        private const val OPEN_SOURCE_LINK = "https://github.com/proxer/ProxerAndroid"

        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }

    private lateinit var hentaiPreference: TwoStatePreference

    override fun onCreatePreferencesFix(bundle: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        findPreference(PreferenceHelper.PREFERENCE_LICENCES).setOnPreferenceClickListener {
            LibsBuilder().withAboutVersionShownName(true)
                    .withAboutDescription(getString(R.string.about_description))
                    .withAboutIconShown(true)
                    .withAutoDetect(false)
                    .withAboutAppName(getString(R.string.app_name))
                    .withLibraries(*LIBRARIES)
                    .withExcludedLibraries(*EXCLUDED_LIBRARIES)
                    .withFields(R.string::class.java.fields)
                    .withActivityStyle(getAboutLibrariesActivityStyle())
                    .withActivityTitle(context.getString(R.string.about_libraries_title))
                    .start(context)

            true
        }

        findPreference(PreferenceHelper.PREFERENCE_OPEN_SOURCE).setOnPreferenceClickListener {
            Utils.viewLink(context, OPEN_SOURCE_LINK)

            true
        }

        hentaiPreference = findPreference(PreferenceHelper.PREFERENCE_HENTAI) as TwoStatePreference
        hentaiPreference.setOnPreferenceClickListener { preference ->
            if ((hentaiPreference).isChecked) {
                hentaiPreference.isChecked = false

                HentaiConfirmationDialog.show(activity as AppCompatActivity)
            }

            true
        }
    }

    override fun onBackPressed() = false

    override fun onResume() {
        super.onResume()

        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()

        MainApplication.refWatcher.watch(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            PreferenceHelper.PREFERENCE_NEWS_NOTIFICATIONS -> {
                ServiceHelper.retrieveNewsLater(context)
            }

            PreferenceHelper.PREFERENCE_NEWS_NOTIFICATIONS_INTERVAL -> {
                ServiceHelper.retrieveNewsLater(context)
            }

            PreferenceHelper.PREFERENCE_HENTAI -> {
                if (PreferenceHelper.isHentaiAllowed(context)) {
                    hentaiPreference.isChecked = true
                }
            }

            PreferenceHelper.PREFERENCE_NIGHT_MODE -> {
                AppCompatDelegate.setDefaultNightMode(PreferenceHelper.getNightMode(context))

                activity.recreate()
            }
        }
    }

    private fun getAboutLibrariesActivityStyle(): Libs.ActivityStyle {
        return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> Libs.ActivityStyle.LIGHT_DARK_TOOLBAR
            Configuration.UI_MODE_NIGHT_YES -> Libs.ActivityStyle.DARK
            Configuration.UI_MODE_NIGHT_UNDEFINED -> Libs.ActivityStyle.LIGHT_DARK_TOOLBAR
            else -> throw RuntimeException("Unknown mode")
        }
    }
}
