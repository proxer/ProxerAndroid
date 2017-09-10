package me.proxer.app.news

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Single
import io.reactivex.rxkotlin.plusAssign
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.base.PagedContentViewModel
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.notifications.NewsArticle

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class NewsViewModel : PagedContentViewModel<NewsArticle>() {

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
                .markAsRead(page == 0)

    init {
        disposables += bus.register(NewsNotificationEvent::class.java).subscribe()
    }
}
