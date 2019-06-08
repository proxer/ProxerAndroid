package me.proxer.app.util.http

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import me.proxer.app.exception.NotConnectedException
import me.proxer.app.util.compat.isConnected
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.KoinComponent

/**
 * @author Ruben Gees
 */
class ConnectivityInterceptor(context: Context) : Interceptor, KoinComponent {

    private val connectivityManager = requireNotNull(context.getSystemService<ConnectivityManager>())

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!connectivityManager.isConnected) {
            throw NotConnectedException()
        }

        return chain.proceed(chain.request())
    }
}
