package me.proxer.app.anime.resolver

/**
 * @author Ruben Gees
 */
object StreamResolverFactory {

    private val resolvers = arrayOf(CrunchyrollStreamResolver(), ProxerStreamResolver())

    fun resolverFor(name: String) = resolvers.find { it.supports(name) }
}
