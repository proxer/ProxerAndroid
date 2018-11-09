package me.proxer.app.news

import io.reactivex.Single
import me.proxer.app.newbase.paged.NewBaseEndpointDataSource
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.api.ProxerApi
import me.proxer.library.api.notifications.NewsEndpoint
import me.proxer.library.entity.notifications.NewsArticle

/**
 * @author Ruben Gees
 */
class NewsDataSource(
    api: ProxerApi,
    private val storageHelper: StorageHelper
) : NewBaseEndpointDataSource<NewsEndpoint, NewsArticle>(api.notifications().news()) {

    override fun prepareEndpoint(endpoint: NewsEndpoint, page: Int, loadSize: Int): NewsEndpoint {
        return endpoint.markAsRead(page == 0)
    }

    override fun prepareCall(page: Int, loadSize: Int): Single<List<NewsArticle>> {
        val call = super.prepareCall(page, loadSize)

        return when {
            page <= 0 -> call.doOnSuccess {
                if (it.isNotEmpty()) storageHelper.lastNewsDate = it.first().date
            }
            else -> call
        }
    }
}
