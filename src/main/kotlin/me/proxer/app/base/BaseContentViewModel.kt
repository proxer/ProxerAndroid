package me.proxer.app.base

import io.reactivex.Single
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.api.Endpoint

/**
 * @author Ruben Gees
 */
abstract class BaseContentViewModel<T : Any> : BaseViewModel<T>() {

    override val dataSingle: Single<T>
        get() = Single.fromCallable { validate() }
            .flatMap { endpoint.buildSingle() }

    protected abstract val endpoint: Endpoint<T>
}
