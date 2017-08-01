package me.proxer.app.news

import android.app.Application
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.PagedContentViewModel
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entitiy.notifications.NewsArticle

/**
 * @author Ruben Gees
 */
class NewsViewModel(application: Application) : PagedContentViewModel<NewsArticle>(application) {

    override val itemsOnPage = 15
    override val endpoint: PagingLimitEndpoint<List<NewsArticle>>
        get() = api.notifications()
                .news()
}
