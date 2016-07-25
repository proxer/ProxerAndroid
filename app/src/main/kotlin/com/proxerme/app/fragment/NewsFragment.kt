package com.proxerme.app.fragment

import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import android.widget.ImageView
import com.proxerme.app.activity.DashboardActivity
import com.proxerme.app.activity.ImageDetailActivity
import com.proxerme.app.adapter.NewsAdapter
import com.proxerme.app.helper.NotificationHelper
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.ProxerConnection
import com.proxerme.library.connection.notifications.entitiy.News
import com.proxerme.library.connection.notifications.request.NewsRequest
import com.proxerme.library.info.ProxerTag
import com.proxerme.library.info.ProxerUrlHolder

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class NewsFragment : PagingFragment() {

    companion object {
        fun newInstance(): NewsFragment {
            return NewsFragment()
        }
    }

    lateinit override var layoutManager: StaggeredGridLayoutManager
    lateinit private var adapter: NewsAdapter

    override val section: SectionManager.Section = SectionManager.Section.NEWS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.cancelNotification(context, NotificationHelper.NEWS_NOTIFICATION)

        adapter = NewsAdapter(savedInstanceState)
        adapter.callback = object : NewsAdapter.OnNewsInteractionListener() {
            override fun onNewsClick(v: View, news: News) {
                (activity as DashboardActivity).showPage(ProxerUrlHolder.getNewsUrl(news.categoryId,
                        news.threadId, "mobile"))
            }

            override fun onNewsImageClick(v: View, news: News) {
                ImageDetailActivity.navigateTo(activity, v as ImageView,
                        ProxerUrlHolder.getNewsImageUrl(news.id, news.imageId))
            }

            override fun onNewsExpanded(v: View, news: News) {
                (activity as DashboardActivity)
                        .setLikelyUrl(ProxerUrlHolder.getNewsUrl(news.categoryId,
                                news.threadId, "mobile"))
            }
        }

        layoutManager = StaggeredGridLayoutManager(Utils.calculateSpanAmount(activity),
                StaggeredGridLayoutManager.VERTICAL)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.setHasFixedSize(true)
        list.layoutManager = layoutManager
        list.adapter = adapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }

    override fun loadPage(number: Int) {
        NewsRequest(number).execute({ result ->
            if (result.item.isNotEmpty()) {
                adapter.addItems(result.item.asList())

                if (number == firstPage) {
                    StorageHelper.lastNewsTime = result.item.first().time
                    StorageHelper.newNews = 0
                }

                notifyPagedLoadFinishedSuccessful(number, result)
            }
        }, { result ->
            notifyPagedLoadFinishedWithError(number, result)
        })
    }

    override fun cancel() {
        ProxerConnection.cancel(ProxerTag.NEWS)
    }

    override fun clear() {
        adapter.clear()
    }
}