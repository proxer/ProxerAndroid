package me.proxer.app.util.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.orhanobut.hawk.Hawk
import me.proxer.app.auth.LocalUser
import me.proxer.app.profile.settings.LocalProfileSettings
import java.io.File

/**
 * @author Ruben Gees
 */
class LocalDataInitializer(
    private val context: Context,
    private val jsonParser: HawkMoshiParser,
    private val preferences: SharedPreferences,
    private val storagePreferences: SharedPreferences
) {

    private companion object {
        private const val VERSION = "version"

        private const val currentVersion = 7
    }

    @Volatile
    private var isInitialized = false

    fun initAndMigrateIfNecessary() {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    // TODO: Remove Hawk in next version.
                    initHawk()

                    val previousVersion = Hawk.get<Int>(VERSION) ?: preferences.getInt(VERSION, 0)

                    if (previousVersion <= 3) {
                        Hawk.deleteAll()
                    }

                    if (previousVersion <= 4) {
                        migrate4To5(preferences)
                    }

                    if (previousVersion <= 5) {
                        migrate5To6(storagePreferences)
                    }

                    if (previousVersion <= 6) {
                        migrate6To7(storagePreferences)
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

        val isTwoFactorAuthenticationEnabled = Hawk.get("two_factor_authentication", false)
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

        Hawk.delete(VERSION)
    }

    private fun migrate5To6(storagePreferences: SharedPreferences) {
        // This migrates Hawk to the new androidx-security-crypto library.
        storagePreferences.edit(commit = true) {
            Hawk.keys().forEach {
                when (val value = Hawk.get<Any>(it)) {
                    is String -> putString(it, value)
                    is Int -> putInt(it, value)
                    is Long -> putLong(it, value)
                    is LocalUser -> putString(it, jsonParser.toJson(value))
                    is LocalProfileSettings -> putString(it, jsonParser.toJson(value))
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.deleteSharedPreferences("Hawk2")
            context.deleteSharedPreferences("crypto.KEY_256")
        } else {
            arrayOf("shared_prefs/Hawk2.xml", "shared_prefs/crypto.KEY_256.xml")
                .map { File(context.filesDir.parent, it) }
                .filter { it.exists() }
                .forEach { it.delete() }
        }
    }

    private fun migrate6To7(storagePreferences: SharedPreferences) {
        // The preference filename was incorrect in the previous version.
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val previousPreferences = EncryptedSharedPreferences.create(
            context,
            "me.proxer.encrypted_preferences.xml",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        storagePreferences.edit(commit = true) {
            previousPreferences.all.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is LocalUser -> putString(key, jsonParser.toJson(value))
                    is LocalProfileSettings -> putString(key, jsonParser.toJson(value))
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.deleteSharedPreferences("me.proxer.encrypted_preferences.xml")
        } else {
            File(context.filesDir.parent, "shared_prefs/me.proxer.encrypted_preferences.xml.xml")
                .let { if (it.exists()) it else null }
                ?.delete()
        }
    }
}
