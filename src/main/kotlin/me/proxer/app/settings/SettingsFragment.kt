package me.proxer.app.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.preference.XpPreferenceFragment
import android.view.View
import me.proxer.app.MainApplication.Companion.refWatcher
import me.proxer.app.R
import me.proxer.app.chat.prv.sync.MessengerWorker
import me.proxer.app.notification.NotificationWorker
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.PreferenceHelper.AGE_CONFIRMATION
import me.proxer.app.util.data.PreferenceHelper.EXTERNAL_CACHE
import me.proxer.app.util.data.PreferenceHelper.NOTIFICATIONS_ACCOUNT
import me.proxer.app.util.data.PreferenceHelper.NOTIFICATIONS_CHAT
import me.proxer.app.util.data.PreferenceHelper.NOTIFICATIONS_INTERVAL
import me.proxer.app.util.data.PreferenceHelper.NOTIFICATIONS_NEWS
import me.proxer.app.util.data.PreferenceHelper.THEME
import me.proxer.app.util.extension.snackbar
import net.xpece.android.support.preference.TwoStatePreference
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.clearTop

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

        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            findPreference(EXTERNAL_CACHE).isVisible = false
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

            EXTERNAL_CACHE -> {
                view?.also { view ->
                    snackbar(view, R.string.fragment_settings_restart_message,
                        actionMessage = R.string.fragment_settings_restart_action,
                        actionCallback = View.OnClickListener {
                            val packageManager = requireContext().packageManager
                            val packageName = requireContext().packageName
                            val intent = packageManager.getLaunchIntentForPackage(packageName).clearTop()

                            startActivity(intent)
                            System.exit(0)
                        })
                }
            }

            THEME -> {
                AppCompatDelegate.setDefaultNightMode(PreferenceHelper.getNightMode(requireContext()))

                requireActivity().recreate()
            }

            NOTIFICATIONS_NEWS, NOTIFICATIONS_ACCOUNT -> {
                updateIntervalNotification()

                NotificationWorker.enqueueIfPossible(requireContext())
            }

            NOTIFICATIONS_CHAT -> MessengerWorker.enqueueSynchronizationIfPossible(requireContext())

            NOTIFICATIONS_INTERVAL -> {
                NotificationWorker.enqueueIfPossible(requireContext())
                MessengerWorker.enqueueSynchronizationIfPossible(requireContext())
            }
        }
    }

    private fun updateIntervalNotification() {
        findPreference(NOTIFICATIONS_INTERVAL).isEnabled = PreferenceHelper
            .areNewsNotificationsEnabled(requireContext()) || PreferenceHelper
            .areAccountNotificationsEnabled(requireContext())
    }
}
