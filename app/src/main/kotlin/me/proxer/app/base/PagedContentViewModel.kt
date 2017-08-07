package me.proxer.app.base

import android.app.Application
import io.reactivex.Single
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.api.PagingLimitEndpoint

/**
 * @author Ruben Gees
 */
abstract class PagedContentViewModel<T>(application: Application) : PagedViewModel<T>(application) {

    override val dataSingle: Single<List<T>>
        get() = Single.fromCallable { validate() }
                .flatMap { endpoint.page(page).limit(itemsOnPage).buildSingle() }

    abstract protected val endpoint: PagingLimitEndpoint<List<T>>
}
