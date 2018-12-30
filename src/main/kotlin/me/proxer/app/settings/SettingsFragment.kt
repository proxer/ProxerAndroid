package me.proxer.app.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import clicks
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import me.proxer.app.BuildConfig
import me.proxer.app.R
import me.proxer.app.chat.prv.sync.MessengerWorker
import me.proxer.app.notification.NotificationWorker
import me.proxer.app.ucp.settings.UcpSettingsActivity
import me.proxer.app.util.KotterKnifePreference
import me.proxer.app.util.bindPreference
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.PreferenceHelper.Companion.AGE_CONFIRMATION
import me.proxer.app.util.data.PreferenceHelper.Companion.EXTERNAL_CACHE
import me.proxer.app.util.data.PreferenceHelper.Companion.HTTP_LOG_LEVEL
import me.proxer.app.util.data.PreferenceHelper.Companion.HTTP_REDACT_TOKEN
import me.proxer.app.util.data.PreferenceHelper.Companion.HTTP_VERBOSE
import me.proxer.app.util.data.PreferenceHelper.Companion.NOTIFICATIONS_ACCOUNT
import me.proxer.app.util.data.PreferenceHelper.Companion.NOTIFICATIONS_CHAT
import me.proxer.app.util.data.PreferenceHelper.Companion.NOTIFICATIONS_INTERVAL
import me.proxer.app.util.data.PreferenceHelper.Companion.NOTIFICATIONS_NEWS
import me.proxer.app.util.data.PreferenceHelper.Companion.THEME
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.clearTop
import me.proxer.app.util.extension.snackbar
import net.xpece.android.support.preference.ListPreference
import net.xpece.android.support.preference.Preference
import net.xpece.android.support.preference.PreferenceCategory
import net.xpece.android.support.preference.TwoStatePreference
import net.xpece.android.support.preference.XpPreferenceFragment
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
    private val storageHelper by inject<StorageHelper>()

    private val profile by bindPreference<Preference>("profile")
    private val ageConfirmation by bindPreference<TwoStatePreference>(AGE_CONFIRMATION)
    private val externalCache by bindPreference<TwoStatePreference>(EXTERNAL_CACHE)
    private val notificationsInterval by bindPreference<ListPreference>(NOTIFICATIONS_INTERVAL)
    private val developerOptions by bindPreference<PreferenceCategory>("developer_options")

    override fun onCreatePreferences2(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        if (
            Environment.isExternalStorageEmulated() ||
            Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED
        ) {
            externalCache.isVisible = false
        }

        if (!BuildConfig.DEBUG && !BuildConfig.LOG) {
            developerOptions.isVisible = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        storageHelper.isLoggedInObservable
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { profile.isEnabled = it }

        profile.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { UcpSettingsActivity.navigateTo(requireActivity()) }

        ageConfirmation.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                if (ageConfirmation.isChecked) {
                    ageConfirmation.isChecked = false

                    AgeConfirmationDialog.show(activity as AppCompatActivity)
                }
            }

        updateIntervalNotificationPreference()

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

    override fun onDestroyView() {
        KotterKnifePreference.reset(this)

        super.onDestroyView()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            AGE_CONFIRMATION -> if (preferenceHelper.isAgeRestrictedMediaAllowed) {
                ageConfirmation.isChecked = true
            }

            THEME -> {
                AppCompatDelegate.setDefaultNightMode(preferenceHelper.nightMode)

                requireActivity().recreate()
            }

            NOTIFICATIONS_NEWS, NOTIFICATIONS_ACCOUNT -> {
                updateIntervalNotificationPreference()

                NotificationWorker.enqueueIfPossible()
            }

            NOTIFICATIONS_CHAT -> MessengerWorker.enqueueSynchronizationIfPossible()

            NOTIFICATIONS_INTERVAL -> {
                NotificationWorker.enqueueIfPossible()
                MessengerWorker.enqueueSynchronizationIfPossible()
            }

            EXTERNAL_CACHE, HTTP_LOG_LEVEL, HTTP_VERBOSE, HTTP_REDACT_TOKEN -> showRestartMessage()
        }
    }

    private fun updateIntervalNotificationPreference() {
        notificationsInterval.isEnabled = preferenceHelper.areNewsNotificationsEnabled ||
            preferenceHelper.areAccountNotificationsEnabled
    }

    private fun showRestartMessage() {
        view?.also { view ->
            snackbar(view, R.string.fragment_settings_restart_message,
                actionMessage = R.string.fragment_settings_restart_action,
                actionCallback = View.OnClickListener {
                    val intent = packageManager.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID)?.clearTop()

                    startActivity(intent)
                    System.exit(0)
                })
        }
    }
}
