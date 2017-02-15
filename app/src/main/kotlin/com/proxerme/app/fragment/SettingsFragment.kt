package com.proxerme.app.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.preference.XpPreferenceFragment
import android.view.View
import com.proxerme.app.R
import com.proxerme.app.application.MainApplication
import com.proxerme.app.dialog.HentaiConfirmationDialog
import com.proxerme.app.helper.PreferenceHelper
import com.proxerme.app.helper.PreferenceHelper.PREFERENCE_HENTAI
import com.proxerme.app.helper.PreferenceHelper.PREFERENCE_NEWS_NOTIFICATIONS
import com.proxerme.app.helper.PreferenceHelper.PREFERENCE_NEWS_NOTIFICATIONS_INTERVAL
import com.proxerme.app.helper.PreferenceHelper.PREFERENCE_NIGHT_MODE
import com.proxerme.app.helper.ServiceHelper
import net.xpece.android.support.preference.TwoStatePreference

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class SettingsFragment : XpPreferenceFragment() {

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }

    private val onSharedPreferenceChangedListener = { _: SharedPreferences, key: String ->
        when (key) {
            PREFERENCE_NEWS_NOTIFICATIONS -> {
                ServiceHelper.retrieveNewsLater(context)
            }

            PREFERENCE_NEWS_NOTIFICATIONS_INTERVAL -> {
                ServiceHelper.retrieveNewsLater(context)
            }

            PREFERENCE_HENTAI -> {
                if (PreferenceHelper.isHentaiAllowed(context)) {
                    (findPreference(PREFERENCE_HENTAI) as TwoStatePreference).isChecked = true
                }
            }

            PREFERENCE_NIGHT_MODE -> {
                AppCompatDelegate.setDefaultNightMode(PreferenceHelper.getNightMode(context))

                activity.recreate()
            }
        }
    }

    override fun onCreatePreferences2(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        findPreference(PREFERENCE_HENTAI).setOnPreferenceClickListener {
            it as TwoStatePreference

            if (it.isChecked) {
                it.isChecked = false

                HentaiConfirmationDialog.show(activity as AppCompatActivity)
            }

            true
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView.isFocusable = false
    }

    override fun onResume() {
        super.onResume()

        preferenceManager.sharedPreferences
                .registerOnSharedPreferenceChangeListener(onSharedPreferenceChangedListener)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences
                .unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangedListener)

        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()

        MainApplication.refWatcher.watch(this)
    }
}
