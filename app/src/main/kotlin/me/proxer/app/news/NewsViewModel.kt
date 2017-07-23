package me.proxer.app.news

import android.app.Application
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.PagedViewModel
import me.proxer.library.entitiy.notifications.NewsArticle

/**
 * @author Ruben Gees
 */
class NewsViewModel(application: Application) : PagedViewModel<NewsArticle>(application) {

    override val itemsOnPage = 15

    override fun constructApiCall() = api.notifications()
            .news()
            .page(page)
            .limit(itemsOnPage)
            .build()
}
