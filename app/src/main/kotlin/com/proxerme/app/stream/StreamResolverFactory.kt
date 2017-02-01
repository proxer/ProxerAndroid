package com.proxerme.app.stream

import com.proxerme.app.stream.resolver.*
import okhttp3.HttpUrl

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
object StreamResolverFactory {

    private val resolvers = arrayOf(Mp4UploadResolver(), StreamcloudResolver(),
            DailyMotionStreamResolver(), NovamovStreamResolver(), ProxerProductStreamResolver(),
            CrunchyrollResolver(), AkibaPassResolver(), ViewsterResolver(), AnimeOnDemandResolver(),
            ClipfishResolver())

    fun getResolverFor(url: HttpUrl): StreamResolver? {
        return resolvers.firstOrNull { it.appliesTo(url) }
    }
}