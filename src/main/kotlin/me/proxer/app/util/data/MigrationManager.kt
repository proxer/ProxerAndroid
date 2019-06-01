package me.proxer.app.util.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import com.orhanobut.hawk.Hawk
import me.proxer.app.auth.LocalUser
import me.proxer.app.ucp.settings.LocalUcpSettings
import java.io.File

/**
 * @author Ruben Gees
 */
class MigrationManager(
    private val context: Context,
    private val hawkMoshiParser: HawkMoshiParser,
    private val secureSharedPreferences: SharedPreferences
) {

    private companion object {
        private const val VERSION = "version"

        private const val currentVersion = 5
    }

    private var isInitialized = false

    @Synchronized
    fun migrateIfNecessary() {
        if (!isInitialized) {
            Hawk.init(context)
                .setParser(hawkMoshiParser)
                .setConverter(null)
                .build()

            val previousVersion = Hawk.get<Int>(VERSION) ?: secureSharedPreferences.getInt(VERSION, 0)

            if (previousVersion <= 3) {
                Hawk.deleteAll()
            }

            // TODO: Remove in next version.
            if (previousVersion <= 4) {
                secureSharedPreferences.edit(commit = true) {
                    Hawk.keys().forEach {
                        when (val value = Hawk.get<Any>(it)) {
                            is String -> putString(it, value)
                            is Int -> putInt(it, value)
                            is Long -> putLong(it, value)
                            is LocalUser -> putString(it, hawkMoshiParser.toJson(value))
                            is LocalUcpSettings -> putString(it, hawkMoshiParser.toJson(value))
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.deleteSharedPreferences("Hawk2")
                } else {
                    File(context.filesDir.parent, "shared_prefs/Hawk2.xml").let {
                        if (it.exists()) it.delete()
                    }
                }
            }

            if (previousVersion != currentVersion) {
                secureSharedPreferences.edit(commit = true) {
                    putInt(VERSION, currentVersion)
                }
            }

            isInitialized = true
        }
    }
}
