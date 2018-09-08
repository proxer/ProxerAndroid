package me.proxer.app.anime.resolver

import io.reactivex.Single

/**
 * @author Ruben Gees
 */
interface StreamResolver {

    val name: String
    val ignore: Boolean get() = false
    val internalPlayerOnly: Boolean get() = false

    fun supports(name: String) = name.equals(this.name, true)
    fun resolve(id: String): Single<StreamResolutionResult>
}
