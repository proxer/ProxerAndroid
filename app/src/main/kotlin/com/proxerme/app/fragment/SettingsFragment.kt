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
import com.proxerme.app.util.openHttpPage
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class SettingsFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private val LIBRARIES = arrayOf("glide", "jodatimeandroid", "hawk", "materialdialogs",
                "eventbus", "circleimageview", "okhttp", "leakcanary", "anko", "moshi",
                "android_textview_linkbuilder", "androidflowlayout", "kotterknife")
        private val EXCLUDED_LIBRARIES = arrayOf("fastadapter", "materialize")
        private val OPEN_SOURCE_LINK = HttpUrl.Builder()
                .scheme("https")
                .host("github.com")
                .addPathSegment("proxer")
                .addPathSegment("ProxerAndroid")
                .build()

        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }

    private lateinit var customTabsHelper: CustomTabsHelperFragment

    private lateinit var hentaiPreference: TwoStatePreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        customTabsHelper = CustomTabsHelperFragment.attachTo(this)
    }

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
            showPage(OPEN_SOURCE_LINK)

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

    fun showPage(url: HttpUrl) {
        customTabsHelper.openHttpPage(activity, url)
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
