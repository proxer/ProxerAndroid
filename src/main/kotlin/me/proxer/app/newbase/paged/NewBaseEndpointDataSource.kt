package me.proxer.app.newbase.paged

import io.reactivex.Single
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.api.PagingLimitEndpoint

/**
 * @author Ruben Gees
 */
abstract class NewBaseEndpointDataSource<E : PagingLimitEndpoint<List<T>>, T>(
    private val endpoint: E
) : NewBaseDataSource<T>() {

    override fun prepareCall(page: Int, loadSize: Int): Single<List<T>> {
        @Suppress("UNCHECKED_CAST")
        val endpointToUse = endpoint.page(page).limit(loadSize) as E

        return prepareEndpoint(endpointToUse, page, loadSize).buildSingle()
    }

    protected open fun prepareEndpoint(endpoint: E, page: Int, loadSize: Int): E = endpoint
}
