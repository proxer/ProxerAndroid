package me.proxer.app.util.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.orhanobut.hawk.Hawk

/**
 * @author Ruben Gees
 */
class LocalDataInitializer(private val context: Context, private val jsonParser: HawkMoshiParser) {

    private companion object {
        private const val VERSION = "version"

        private const val currentVersion = 5
    }

    @Volatile
    private var isInitialized = false

    fun initAndMigrateIfNecessary() {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    initHawk()

                    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                    val previousVersion = Hawk.get<Int>(VERSION) ?: preferences.getInt(VERSION, 0)

                    if (previousVersion <= 3) {
                        Hawk.deleteAll()
                    }

                    if (previousVersion <= 4) {
                        migrate4To5(preferences)
                    }

                    if (previousVersion != currentVersion) {
                        preferences.edit(commit = true) { putInt(VERSION, currentVersion) }
                    }

                    isInitialized = true
                }
            }
        }
    }

    private fun initHawk() {
        if (!Hawk.isBuilt()) {
            Hawk.init(context)
                .setParser(jsonParser)
                .setConverter(null)
                .build()
        }
    }

    private fun migrate4To5(preferences: SharedPreferences) {
        // On older versions of the App, this information is saved in the encrypted preference.
        // In the current version that preference is tied to the login state of the user, but these are preferences
        // which should be shared between users.
        val castIntroductoryOverlayShown = Hawk.get("cast_introductory_overlay_shown", false)
        val launches = Hawk.get("launches", 0)
        val rated = Hawk.get("rated", false)

        val isTwoFactorAuthenticationEnabled = Hawk.get("two_factor_authentication", true)
        val lastTagUpdateDate = Hawk.get("last_tag_update_date", 0L)
        val lastNewsDate = Hawk.get("last_news_date", 0L)

        preferences.edit(commit = true) {
            putBoolean("cast_introductory_overlay_shown", castIntroductoryOverlayShown)
            putInt("launches", launches)
            putBoolean("rated", rated)

            putBoolean("two_factor_authentication", isTwoFactorAuthenticationEnabled)
            putLong("last_tag_update_date", lastTagUpdateDate)
            putLong("last_news_date", lastNewsDate)
        }

        Hawk.delete("cast_introductory_overlay_shown")
        Hawk.delete("launches")
        Hawk.delete("rated")

        Hawk.delete("two_factor_authentication")
        Hawk.delete("last_tag_update_date")
        Hawk.delete("last_news_date")
    }
}
