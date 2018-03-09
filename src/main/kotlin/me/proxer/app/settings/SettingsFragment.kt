package me.proxer.app.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.preference.XpPreferenceFragment
import android.view.View
import me.proxer.app.MainApplication.Companion.refWatcher
import me.proxer.app.R
import me.proxer.app.chat.sync.ChatJob
import me.proxer.app.notification.NotificationJob
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.PreferenceHelper.AGE_CONFIRMATION
import me.proxer.app.util.data.PreferenceHelper.NOTIFICATIONS_ACCOUNT
import me.proxer.app.util.data.PreferenceHelper.NOTIFICATIONS_CHAT
import me.proxer.app.util.data.PreferenceHelper.NOTIFICATIONS_INTERVAL
import me.proxer.app.util.data.PreferenceHelper.NOTIFICATIONS_NEWS
import me.proxer.app.util.data.PreferenceHelper.THEME
import net.xpece.android.support.preference.TwoStatePreference
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class SettingsFragment : XpPreferenceFragment(), OnSharedPreferenceChangeListener {

    companion object {
        fun newInstance() = SettingsFragment().apply {
            arguments = bundleOf()
        }
    }

    override fun onCreatePreferences2(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        findPreference(AGE_CONFIRMATION).setOnPreferenceClickListener {
            it as TwoStatePreference

            if (it.isChecked) {
                it.isChecked = false

                AgeConfirmationDialog.show(activity as AppCompatActivity)
            }

            true
        }

        updateIntervalNotification()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView.isFocusable = false
    }

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

        refWatcher.watch(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            AGE_CONFIRMATION -> if (PreferenceHelper.isAgeRestrictedMediaAllowed(requireContext())) {
                (findPreference(AGE_CONFIRMATION) as TwoStatePreference).isChecked = true
            }

            THEME -> {
                AppCompatDelegate.setDefaultNightMode(PreferenceHelper.getNightMode(requireContext()))

                requireActivity().recreate()
            }

            NOTIFICATIONS_NEWS, NOTIFICATIONS_ACCOUNT -> {
                updateIntervalNotification()

                NotificationJob.scheduleIfPossible(requireContext())
            }

            NOTIFICATIONS_CHAT -> ChatJob.scheduleSynchronizationIfPossible(requireContext())

            NOTIFICATIONS_INTERVAL -> {
                NotificationJob.scheduleIfPossible(requireContext())
                ChatJob.scheduleSynchronizationIfPossible(requireContext())
            }
        }
    }

    private fun updateIntervalNotification() {
        findPreference(NOTIFICATIONS_INTERVAL).isEnabled = PreferenceHelper
            .areNewsNotificationsEnabled(requireContext()) || PreferenceHelper
            .areAccountNotificationsEnabled(requireContext())
    }
}
