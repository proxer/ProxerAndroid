package me.proxer.app.base

import io.reactivex.Single
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.api.PagingLimitEndpoint

/**
 * @author Ruben Gees
 */
abstract class PagedContentViewModel<T> : PagedViewModel<T>() {

    override val dataSingle: Single<List<T>>
        get() = Single.fromCallable { validate() }
            .flatMap { endpoint.page(page).limit(itemsOnPage).buildSingle() }

    protected abstract val endpoint: PagingLimitEndpoint<List<T>>
}
