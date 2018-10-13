package me.proxer.app.util.extension

import android.os.StrictMode
import me.proxer.app.BuildConfig

inline fun <T> permitSlowCalls(action: () -> T): T {
    if (BuildConfig.DEBUG) {
        val oldThreadPolicy = StrictMode.getThreadPolicy()
        val newThreadPolicy = StrictMode.ThreadPolicy.Builder(oldThreadPolicy)
            .permitCustomSlowCalls()
            .build()

        StrictMode.setThreadPolicy(newThreadPolicy)

        val result = action()

        StrictMode.setThreadPolicy(oldThreadPolicy)

        return result
    } else {
        return action()
    }
}
