package me.proxer.app.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import me.proxer.app.BuildConfig
import me.proxer.app.R
import me.proxer.app.chat.prv.sync.MessengerWorker
import me.proxer.app.notification.NotificationWorker
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.PreferenceHelper.Companion.AGE_CONFIRMATION
import me.proxer.app.util.data.PreferenceHelper.Companion.EXTERNAL_CACHE
import me.proxer.app.util.data.PreferenceHelper.Companion.NOTIFICATIONS_ACCOUNT
import me.proxer.app.util.data.PreferenceHelper.Companion.NOTIFICATIONS_CHAT
import me.proxer.app.util.data.PreferenceHelper.Companion.NOTIFICATIONS_INTERVAL
import me.proxer.app.util.data.PreferenceHelper.Companion.NOTIFICATIONS_NEWS
import me.proxer.app.util.data.PreferenceHelper.Companion.THEME
import me.proxer.app.util.extension.snackbar
import net.xpece.android.support.preference.TwoStatePreference
import net.xpece.android.support.preference.XpPreferenceFragment
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.clearTop
import org.koin.android.ext.android.inject

/**
 * @author Ruben Gees
 */
class SettingsFragment : XpPreferenceFragment(), OnSharedPreferenceChangeListener {

    companion object {
        fun newInstance() = SettingsFragment().apply {
            arguments = bundleOf()
        }
    }

    private val packageManager by inject<PackageManager>()
    private val preferenceHelper by inject<PreferenceHelper>()

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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            AGE_CONFIRMATION -> if (preferenceHelper.isAgeRestrictedMediaAllowed) {
                (findPreference(AGE_CONFIRMATION) as TwoStatePreference).isChecked = true
            }

            EXTERNAL_CACHE -> view?.also { view ->
                snackbar(view, R.string.fragment_settings_restart_message,
                    actionMessage = R.string.fragment_settings_restart_action,
                    actionCallback = View.OnClickListener {
                        val intent = packageManager.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID)?.clearTop()

                        startActivity(intent)
                        System.exit(0)
                    })
            }

            THEME -> {
                AppCompatDelegate.setDefaultNightMode(preferenceHelper.nightMode)

                requireActivity().recreate()
            }

            NOTIFICATIONS_NEWS, NOTIFICATIONS_ACCOUNT -> {
                updateIntervalNotification()

                NotificationWorker.enqueueIfPossible()
            }

            NOTIFICATIONS_CHAT -> MessengerWorker.enqueueSynchronizationIfPossible()

            NOTIFICATIONS_INTERVAL -> {
                NotificationWorker.enqueueIfPossible()
                MessengerWorker.enqueueSynchronizationIfPossible()
            }
        }
    }

    private fun updateIntervalNotification() {
        findPreference(NOTIFICATIONS_INTERVAL).isEnabled = preferenceHelper.areNewsNotificationsEnabled ||
            preferenceHelper.areAccountNotificationsEnabled
    }
}
