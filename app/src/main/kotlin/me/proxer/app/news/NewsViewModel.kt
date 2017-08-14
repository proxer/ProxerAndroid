package me.proxer.app.news

import android.app.Application
import io.reactivex.Single
import io.reactivex.rxkotlin.plusAssign
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.base.PagedContentViewModel
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entitiy.notifications.NewsArticle

/**
 * @author Ruben Gees
 */
class NewsViewModel(application: Application) : PagedContentViewModel<NewsArticle>(application) {

    override val itemsOnPage = 15

    override val dataSingle: Single<List<NewsArticle>>
        get() = super.dataSingle.doOnSuccess {
            if (page == 0) {
                it.firstOrNull()?.date?.let {
                    StorageHelper.lastNewsDate = it
                }
            }
        }

    override val endpoint: PagingLimitEndpoint<List<NewsArticle>>
        get() = api.notifications().news()

    init {
        disposables += bus.register(NewsNotificationEvent::class.java).subscribe()
    }
}
