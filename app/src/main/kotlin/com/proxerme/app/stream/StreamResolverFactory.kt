package com.proxerme.app.stream

import com.proxerme.app.stream.resolver.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
object StreamResolverFactory {

    private val resolvers = arrayOf(Mp4UploadResolver(), StreamcloudResolver(),
            DailyMotionStreamResolver(), NovamovStreamResolver(), CrunchyrollResolver(),
            AkibaPassResolver(), ViewsterResolver(), AnimeOnDemandResolver(), ClipfishResolver(),
            DaisukiResolver(), AmazonPrimeVideoResolver(), YourUploadResolver(), CodeResolver(),
            ProsiebenMAXXResolver(), YoutubeResolver(), VideoWeedStreamResolver())

    fun getResolverFor(name: String): StreamResolver? {
        return resolvers.firstOrNull { it.appliesTo(name) }
    }
}