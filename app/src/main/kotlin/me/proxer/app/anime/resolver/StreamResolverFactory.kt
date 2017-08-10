package me.proxer.app.anime.resolver

/**
 * @author Ruben Gees
 */
object StreamResolverFactory {

    private val resolvers = arrayOf(AkibaPassStreamResolver(), AmazonPrimeVideoStreamResolver(),
            AnimeOnDemandStreamResolver(), AuravidStreamResolver(), ClipfishStreamResolver(), CodeStreamResolver(),
            CrunchyrollStreamResolver(), DailymotionStreamResolver(), Mp4UploadStreamResolver(), MyviStreamResolver(),
            OpenloadStreamResolver(), ProsiebenMAXXStreamResolver(), ProxerStreamResolver(),
            StreamcloudStreamResolver(), VideoweedStreamResolver(), ViewsterStreamResolver(),
            YourUploadStreamResolver(), YouTubeStreamResolver())

    fun resolverFor(name: String) = resolvers.find { it.supports(name) }
}
