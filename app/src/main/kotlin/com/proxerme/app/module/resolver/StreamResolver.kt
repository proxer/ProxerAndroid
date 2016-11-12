package com.proxerme.app.module.resolver

import okhttp3.Response
import java.io.IOException

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class StreamResolver {

    abstract val name: String

    abstract fun resolve(url: String): String

    @Throws(IOException::class)
    protected fun validateAndGetResult(response: Response): String {
        if (response.isSuccessful) {
            val body = response.body()
            val content = body.string()

            body.close()

            return content
        } else {
            throw IOException()
        }
    }

}