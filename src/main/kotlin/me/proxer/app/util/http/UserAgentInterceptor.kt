package me.proxer.app.util.http

import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.library.util.ProxerUrls.hasProxerHost
import okhttp3.Interceptor
import okhttp3.Response

/**
 * @author Ruben Gees
 */
class UserAgentInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val newRequest = when (request.header("User-Agent") == null && request.url.hasProxerHost) {
            true ->
                request.newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()

            false -> request
        }

        return chain.proceed(newRequest)
    }
}
