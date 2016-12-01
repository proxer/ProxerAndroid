package com.proxerme.app.fragment.news

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.proxerme.app.activity.DashboardActivity
import com.proxerme.app.activity.ImageDetailActivity
import com.proxerme.app.adapter.news.NewsAdapter
import com.proxerme.app.adapter.news.NewsAdapter.NewsAdapterCallback
import com.proxerme.app.fragment.framework.PagedLoadingFragment
import com.proxerme.app.helper.NotificationHelper
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.LoadingTask
import com.proxerme.app.task.framework.ListenableTask
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.notifications.entitiy.News
import com.proxerme.library.connection.notifications.request.NewsRequest
import com.proxerme.library.info.ProxerUrlHolder

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class NewsFragment : PagedLoadingFragment<News>() {

    companion object {
        fun newInstance(): NewsFragment {
            return NewsFragment()
        }
    }

    override val section = Section.NEWS
    override val itemsOnPage = 15

    override lateinit var layoutManager: GridLayoutManager
    override lateinit var adapter: NewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = NewsAdapter()
        adapter.callback = object : NewsAdapterCallback() {
            override fun onItemClick(item: News) {
                (activity as DashboardActivity).showPage(ProxerUrlHolder.getNewsUrl(item.categoryId,
                        item.threadId, "mobile"))
            }

            override fun onNewsImageClick(view: ImageView, item: News) {
                ImageDetailActivity.navigateTo(activity, view,
                        ProxerUrlHolder.getNewsImageUrl(item.id, item.imageId))
            }

            override fun onNewsExpanded(item: News) {
                (activity as DashboardActivity)
                        .setLikelyUrl(ProxerUrlHolder.getNewsUrl(item.categoryId,
                                item.threadId, "mobile"))
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        layoutManager = GridLayoutManager(context, Utils.calculateSpanAmount(activity))

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        NotificationHelper.cancelNotification(context, NotificationHelper.NEWS_NOTIFICATION)
    }

    override fun constructTask(pageCallback: () -> Int): ListenableTask<Array<News>> {
        return LoadingTask({ NewsRequest(pageCallback.invoke()) })
    }
}