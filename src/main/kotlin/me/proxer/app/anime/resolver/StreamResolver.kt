package me.proxer.app.anime.resolver

import com.squareup.moshi.Moshi
import io.reactivex.Single
import me.proxer.app.util.extension.safeInject
import me.proxer.library.ProxerApi
import okhttp3.OkHttpClient

/**
 * @author Ruben Gees
 */
abstract class StreamResolver {

    abstract val name: String

    open val resolveEarly: Boolean get() = false
    open val ignore: Boolean get() = false

    protected val api by safeInject<ProxerApi>()
    protected val client by safeInject<OkHttpClient>()
    protected val moshi by safeInject<Moshi>()

    open fun supports(name: String) = name.equals(this.name, true)
    abstract fun resolve(id: String): Single<StreamResolutionResult>
}
