package com.proxerme.app.stream

import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import okhttp3.Response
import java.io.IOException

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class StreamResolver {

    abstract val name: String

    open fun appliesTo(name: String) = name.contains(this.name, ignoreCase = true)
    abstract fun resolve(url: String): StreamResolutionResult

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