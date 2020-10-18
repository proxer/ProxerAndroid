package me.proxer.app.util.http

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.getSystemService
import me.proxer.app.exception.NotConnectedException
import me.proxer.app.util.compat.isConnected
import okhttp3.Interceptor
import okhttp3.Response

/**
 * @author Ruben Gees
 */
class ConnectivityInterceptor(context: Context) : Interceptor {

    private val connectivityManager = requireNotNull(context.getSystemService<ConnectivityManager>())

    private val hasConnectionSettings = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).resolveActivity(context.packageManager) != null ||
        Intent(Settings.ACTION_WIRELESS_SETTINGS).resolveActivity(context.packageManager) != null

    override fun intercept(chain: Interceptor.Chain): Response {
        if (hasConnectionSettings && !connectivityManager.isConnected) {
            throw NotConnectedException()
        }

        return chain.proceed(chain.request())
    }
}
