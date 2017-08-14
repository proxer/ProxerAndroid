package me.proxer.app.news

import android.app.Application
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.base.PagedContentViewModel
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entitiy.notifications.NewsArticle

/**
 * @author Ruben Gees
 */
class NewsViewModel(application: Application) : PagedContentViewModel<NewsArticle>(application) {

    override val itemsOnPage = 15

    override val endpoint: PagingLimitEndpoint<List<NewsArticle>>
        get() = api.notifications().news()

    init {
        disposables += bus.register(NewsNotificationEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (isLoading.value != true) {
                        data.value?.let { existingData ->
                            refreshError.value = null
                            error.value = null
                            data.value = it.news + existingData.filter { item ->
                                it.news.find { oldItem -> areItemsTheSame(oldItem, item) } == null
                            }
                        }

                        page = data.value?.size?.div(itemsOnPage) ?: 0
                    }
                }
    }
}
