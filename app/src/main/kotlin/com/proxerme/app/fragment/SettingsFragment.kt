package com.proxerme.app.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.Preference
import android.support.v7.preference.TwoStatePreference
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.proxerme.app.R
import com.proxerme.app.dialog.HentaiConfirmationDialog
import com.proxerme.app.helper.NotificationHelper
import com.proxerme.app.helper.PreferenceHelper
import com.proxerme.app.interfaces.OnActivityListener
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class SettingsFragment : PreferenceFragmentCompat(), OnActivityListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private val LIBRARIES: Array<String> = arrayOf("glide", "jodatimeandroid", "bridge",
                "hawk", "materialdialogs", "eventbus", "circleimageview",
                "priorityjobqueue")
        private val EXCLUDED_LIBRARIES: Array<String> = arrayOf("fastadapter", "materialize")

        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }

    private lateinit var hentaiPreference: TwoStatePreference

    override fun onCreatePreferencesFix(bundle: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        findPreference(PreferenceHelper.PREFERENCE_LICENCES).onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    LibsBuilder().withAboutVersionShownName(true)
                            .withAboutDescription(getString(R.string.about_description))
                            .withAboutIconShown(true)
                            .withAutoDetect(false)
                            .withAboutAppName(getString(R.string.app_name))
                            .withLibraries(*LIBRARIES)
                            .withExcludedLibraries(*EXCLUDED_LIBRARIES)
                            .withFields(R.string::class.java.fields)
                            .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                            .withActivityTitle(context.getString(R.string.about_libraries_title))
                            .start(context)

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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            PreferenceHelper.PREFERENCE_NEWS_NOTIFICATIONS -> {
                NotificationHelper.retrieveNewsLater(context)
            }

            PreferenceHelper.PREFERENCE_MESSAGES_NOTIFICATIONS -> {
                NotificationHelper.retrieveChatLater(context)
            }

            PreferenceHelper.PREFERENCE_NEWS_NOTIFICATIONS_INTERVAL -> {
                NotificationHelper.retrieveNewsLater(context)
            }

            PreferenceHelper.PREFERENCE_HENTAI -> {
                if (PreferenceHelper.isHentaiAllowed(context)) {
                    hentaiPreference.isChecked = true
                }
            }
        }
    }
}
