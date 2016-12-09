package com.proxerme.app.stream

import com.proxerme.app.stream.resolver.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
object StreamResolverFactory {

    private val resolvers = arrayOf(Mp4UploadResolver(), StreamcloudResolver(),
            DailyMotionStreamResolver(), NovamovStreamResolver(), ProxerProductStreamResolver())

    fun hasResolverFor(url: String): Boolean {
        return resolvers.any { it.appliesTo(url) }
    }

    fun getResolverFor(url: String): StreamResolver? {
        return resolvers.firstOrNull { it.appliesTo(url) }
    }

}