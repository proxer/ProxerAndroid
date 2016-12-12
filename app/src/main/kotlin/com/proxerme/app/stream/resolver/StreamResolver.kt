package com.proxerme.app.stream.resolver

import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import okhttp3.HttpUrl
import okhttp3.Response
import java.io.IOException

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class StreamResolver {

    abstract val name: String

    abstract fun resolve(url: HttpUrl): StreamResolutionResult

    open fun appliesTo(url: HttpUrl): Boolean {
        return url.host().contains(name, ignoreCase = true)
    }

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