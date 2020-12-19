package me.proxer.app.anime.resolver

/**
 * @author Ruben Gees
 */
object StreamResolverFactory {

    private val resolvers = arrayOf(
        AmazonPrimeVideoStreamResolver, AnimeOnDemandStreamResolver, MessageStreamResolver, CrunchyrollStreamResolver,
        DailymotionStreamResolver, Mp4UploadStreamResolver, NetflixStreamResolver, ProsiebenMAXXStreamResolver,
        ProxerStreamResolver, ProxerStreamCFResolver, SteamStreamResolver, StreamcloudStreamResolver,
        YourUploadStreamResolver, YouTubeStreamResolver
    )

    fun resolverFor(name: String) = resolvers.find { it.supports(name) }
}
