package com.proxerme.app.fragment

import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import android.widget.ImageView
import com.afollestad.bridge.Request
import com.proxerme.app.activity.DashboardActivity
import com.proxerme.app.activity.ImageDetailActivity
import com.proxerme.app.adapter.NewsAdapter
import com.proxerme.app.event.NewsEvent
import com.proxerme.app.helper.NotificationHelper
import com.proxerme.app.helper.StorageHelper
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
class NewsFragment : PagingFragment() {

    companion object {
        fun newInstance(): NewsFragment {
            return NewsFragment()
        }
    }

    lateinit override var layoutManager: StaggeredGridLayoutManager
    lateinit private var adapter: NewsAdapter

    override val section: SectionManager.Section = SectionManager.Section.NEWS

    private var request: Request? = null

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

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)

        super.onStop()
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
        request = NewsRequest(number).execute({ result ->
            if (result.item.isNotEmpty()) {
                adapter.addItems(result.item.asList())

                if (number == 0) {
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
        request?.cancel()
    }

    override fun clear() {
        adapter.clear()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNewsReceived(event: NewsEvent) {
        adapter.addItems(event.news)
    }
}