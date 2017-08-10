package me.proxer.app.anime.resolver

/**
 * @author Ruben Gees
 */
object StreamResolverFactory {

    private val resolvers = arrayOf(AkibaPassStreamResolver(), AmazonPrimeVideoStreamResolver(),
            AnimeOnDemandStreamResolver(), AuravidStreamResolver(), ClipfishStreamResolver(), CodeStreamResolver(),
            CrunchyrollStreamResolver(), DailymotionStreamResolver(), Mp4UploadStreamResolver(), MyviStreamResolver(),
            OpenloadStreamResolver(), ProxerStreamResolver(), StreamcloudStreamResolver(), VideoweedStreamResolver(),
            ViewsterStreamResolver(), YourUploadStreamResolver())

    fun resolverFor(name: String) = resolvers.find { it.supports(name) }
}
