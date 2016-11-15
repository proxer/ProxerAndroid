package com.proxerme.app.module

import com.proxerme.app.module.resolver.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
object StreamResolvers {

    private val resolvers = arrayOf(Mp4UploadResolver(), StreamcloudResolver(),
            DailyMotionStreamResolver(), NovamovStreamResolver())

    fun hasResolverFor(url: String): Boolean {
        return resolvers.any { url.contains(it.name, ignoreCase = true) }
    }

    fun getResolverFor(url: String): StreamResolver? {
        return resolvers.firstOrNull { url.contains(it.name, ignoreCase = true) }
    }

}