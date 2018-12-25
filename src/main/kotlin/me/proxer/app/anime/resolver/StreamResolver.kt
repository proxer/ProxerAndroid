package me.proxer.app.anime.resolver

import com.squareup.moshi.Moshi
import io.reactivex.Single
import me.proxer.library.api.ProxerApi
import okhttp3.OkHttpClient
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

/**
 * @author Ruben Gees
 */
abstract class StreamResolver : KoinComponent {

    abstract val name: String

    open val resolveEarly: Boolean get() = false
    open val internalPlayerOnly: Boolean get() = false
    open val ignore: Boolean get() = false

    protected val api by inject<ProxerApi>()
    protected val client by inject<OkHttpClient>()
    protected val moshi by inject<Moshi>()

    open fun supports(name: String) = name.equals(this.name, true)
    abstract fun resolve(id: String): Single<StreamResolutionResult>
}
