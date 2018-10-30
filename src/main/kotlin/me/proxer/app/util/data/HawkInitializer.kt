package me.proxer.app.util.data

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.orhanobut.hawk.Converter
import com.orhanobut.hawk.DataInfo
import com.orhanobut.hawk.Hawk
import me.proxer.app.auth.LocalUser
import timber.log.Timber

/**
 * @author Ruben Gees
 */
class HawkInitializer(private val jsonParser: HawkMoshiParser) {

    private companion object {
        private const val VERSION = "version"

        private const val currentVersion = 1
    }

    fun initAndMigrateIfNecessary(context: Context) {
        initHawk(context)

        val previousVersion = Hawk.get<Int>(VERSION) ?: 0

        if (previousVersion <= 0) {
            migrate0To1(context)
        }

        if (previousVersion != currentVersion) {
            Hawk.put(VERSION, currentVersion)
        }
    }

    private fun initHawk(context: Context) {
        if (!Hawk.isBuilt()) {
            Hawk.init(context)
                .setParser(jsonParser)
                .setConverter(null)
                .build()
        }
    }

    private fun migrate0To1(context: Context) {
        // On older versions of the App, the user is saved in an obfuscated format. Fix this by reading the previous
        // format and saving in the proper format.
        if (Hawk.contains(StorageHelper.USER) && Hawk.get<LocalUser>(StorageHelper.USER) == null) {
            Hawk.init(context)
                .setConverter(UserMigration0To1Converter(jsonParser))
                .build()

            val migrationUser: StorageMigration0To1LocalUser? = Hawk.get(StorageHelper.USER)

            Hawk.destroy()
            initHawk(context)

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

    private class UserMigration0To1Converter(private val jsonParser: HawkMoshiParser) : Converter {
        override fun <T : Any?> toString(value: T): String {
            // This should never be called
            return ""
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any?> fromString(value: String, dataInfo: DataInfo?): T {
            return jsonParser
                .fromJson<StorageMigration0To1LocalUser>(value, StorageMigration0To1LocalUser::class.java) as T
        }
    }
}
