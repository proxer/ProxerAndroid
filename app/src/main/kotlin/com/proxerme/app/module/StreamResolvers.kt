package com.proxerme.app.module

import com.proxerme.app.module.resolver.Mp4UploadResolver
import com.proxerme.app.module.resolver.StreamResolver
import com.proxerme.app.module.resolver.StreamcloudResolver

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
object StreamResolvers {

    private val resolvers = arrayOf(Mp4UploadResolver(), StreamcloudResolver())

    fun hasResolverFor(url: String): Boolean {
        return resolvers.any { url.contains(it.name, ignoreCase = true) }
    }

    fun getResolverFor(url: String): StreamResolver? {
        return resolvers.firstOrNull { url.contains(it.name, ignoreCase = true) }
    }

}