package me.proxer.app.anime.resolver

import io.reactivex.Single

/**
 * @author Ruben Gees
 */
abstract class StreamResolver {

    abstract val name: String

    open fun supports(name: String) = name.equals(this.name, true)
    abstract fun resolve(id: String): Single<StreamResolutionResult>
}
