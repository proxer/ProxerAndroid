package me.proxer.app.util.http

import okhttp3.Interceptor
import okhttp3.Response

/**
 * @author Ruben Gees
 */
class ConnectionCloseInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val newRequest = chain.request().newBuilder()
            .header("Connection", "close")
            .build()

        return chain.proceed(newRequest)
    }
}
