package me.proxer.app.util.data

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.orhanobut.hawk.Converter
import com.orhanobut.hawk.DataInfo
import com.orhanobut.hawk.Hawk
import me.proxer.app.auth.LocalUser
import me.proxer.app.manga.MangaReaderOrientation
import timber.log.Timber

/**
 * @author Ruben Gees
 */
class LocalDataInitializer(private val context: Context, private val jsonParser: HawkMoshiParser) {

    private companion object {
        private const val VERSION = "version"

        private const val currentVersion = 3
    }

    private var isInitialized = false

    @Synchronized
    fun initAndMigrateIfNecessary() {
        if (!isInitialized) {
            initHawk()

            val previousVersion = Hawk.get<Int>(VERSION) ?: 0

            if (previousVersion <= 0) {
                migrate0To1()
            }

            if (previousVersion <= 1) {
                migrate1To2()
            }

            if (previousVersion <= 2) {
                migrate2To3()
            }

            if (previousVersion != currentVersion) {
                Hawk.put(VERSION, currentVersion)
            }

            isInitialized = true
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

    private fun migrate0To1() {
        // On older versions of the App, the user is saved in an obfuscated format. Fix this by reading the previous
        // format and saving in the proper format.
        if (Hawk.contains(StorageHelper.USER) && Hawk.get<LocalUser>(StorageHelper.USER) == null) {
            Hawk.init(context)
                .setConverter(UserMigration0To1Converter(jsonParser))
                .build()

            val migrationUser: LocalDataMigration0To1LocalUser? = Hawk.get(StorageHelper.USER)

            Hawk.destroy()
            initHawk()

            if (migrationUser != null) {
                val newUser = LocalUser(migrationUser.token, migrationUser.id, migrationUser.name, migrationUser.image)

                Hawk.put(StorageHelper.USER, newUser)
            } else {
                Timber.e("Could not migrate user")
            }
        }

        // On older versions of the App, this information is saved as an unencrypted preference. While not critical,
        // these are not preferences and should be hidden from the user.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        if (sharedPreferences.contains(StorageHelper.LAUNCHES)) {
            Hawk.put(StorageHelper.LAUNCHES, sharedPreferences.getInt(StorageHelper.LAUNCHES, 0))

            sharedPreferences.edit(commit = true) { remove(StorageHelper.LAUNCHES) }
        }

        if (sharedPreferences.contains(StorageHelper.RATED)) {
            Hawk.put(StorageHelper.RATED, sharedPreferences.getBoolean(StorageHelper.RATED, false))

            sharedPreferences.edit(commit = true) { remove(StorageHelper.RATED) }
        }
    }

    private fun migrate1To2() {
        // In older versions of the App, there was only a setting if the manga reader should be vertical or not.
        // Now there is an enum with multiple choices.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val wasVertical = sharedPreferences.getBoolean("manga_vertical_reader", true)

        sharedPreferences.edit(commit = true) {
            putInt(
                PreferenceHelper.MANGA_READER_ORIENTATION, when (wasVertical) {
                    true -> MangaReaderOrientation.VERTICAL.ordinal
                    false -> MangaReaderOrientation.LEFT_TO_RIGHT.ordinal
                }
            )
        }
    }

    private fun migrate2To3() {
        // In older versions of the App, there was an additional night mode for automatic switching based on the
        // time. This has been deprecated and thus removed from the App.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val previousNightMode = sharedPreferences.getString("theme", "0")

        sharedPreferences.edit(commit = true) {
            putString(
                PreferenceHelper.THEME, when (previousNightMode) {
                    "1" -> "1"
                    else -> "0"
                }
            )
        }
    }

    private class UserMigration0To1Converter(private val jsonParser: HawkMoshiParser) : Converter {
        override fun <T : Any?> toString(value: T): String {
            // This should never be called
            return ""
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any?> fromString(value: String, dataInfo: DataInfo?): T {
            return jsonParser
                .fromJson<LocalDataMigration0To1LocalUser>(value, LocalDataMigration0To1LocalUser::class.java) as T
        }
    }
}
