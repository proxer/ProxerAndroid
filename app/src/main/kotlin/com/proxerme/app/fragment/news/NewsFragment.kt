package com.proxerme.app.fragment.news

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.View
import android.widget.ImageView
import com.proxerme.app.activity.DashboardActivity
import com.proxerme.app.activity.ImageDetailActivity
import com.proxerme.app.adapter.NewsAdapter
import com.proxerme.app.event.NewsEvent
import com.proxerme.app.fragment.framework.EasyPagingFragment
import com.proxerme.app.helper.NotificationHelper
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.notifications.entitiy.News
import com.proxerme.library.connection.notifications.request.NewsRequest
import com.proxerme.library.info.ProxerUrlHolder
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class NewsFragment : EasyPagingFragment<News>() {

    companion object {

        const val ITEMS_ON_PAGE = 15

        fun newInstance(): NewsFragment {
            return NewsFragment()
        }
    }

    override val section = SectionManager.Section.NEWS
    override val itemsOnPage = ITEMS_ON_PAGE

    override lateinit var layoutManager: GridLayoutManager
    override lateinit var adapter: NewsAdapter

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

        layoutManager = GridLayoutManager(context, Utils.calculateSpanAmount(activity))
    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)

        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }

    override fun constructPagedLoadingRequest(page: Int): LoadingRequest<Array<News>> {
        return LoadingRequest(NewsRequest(page).withLimit(ITEMS_ON_PAGE))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNewsReceived(event: NewsEvent) {
        adapter.insert(event.news)
    }
}