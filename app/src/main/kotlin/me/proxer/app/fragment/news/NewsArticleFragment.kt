package me.proxer.app.fragment.news

import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import android.widget.ImageView
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.ImageDetailActivity
import me.proxer.app.adapter.news.NewsArticleAdapter
import me.proxer.app.adapter.news.NewsArticleAdapter.NewsAdapterCallback
import me.proxer.app.application.GlideApp
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.PagedLoadingFragment
import me.proxer.app.helper.NotificationHelper
import me.proxer.app.helper.StorageHelper
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.DeviceUtils
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.notifications.NewsArticle
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class NewsArticleFragment : PagedLoadingFragment<ProxerCall<List<NewsArticle>>, NewsArticle>() {

    companion object {
        var isActive: Boolean = false
            private set

        fun newInstance(): NewsArticleFragment {
            return NewsArticleFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    override val isSwipeToRefreshEnabled = true
    override val itemsOnPage = 15
    override val emptyResultMessage = R.string.error_no_data_news

    override val layoutManager by lazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity), StaggeredGridLayoutManager.VERTICAL)
    }
    override lateinit var innerAdapter: NewsArticleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = NewsArticleAdapter(savedInstanceState, GlideApp.with(this))
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.callback = object : NewsAdapterCallback {
            override fun onNewsArticleClick(item: NewsArticle) {
                showPage(ProxerUrls.newsWeb(item.categoryId, item.threadId, Device.MOBILE))
            }

            override fun onNewsArticleImageClick(view: ImageView, item: NewsArticle) {
                if (view.drawable != null) {
                    ImageDetailActivity.navigateTo(activity, ProxerUrls.newsImage(item.id, item.image), view)
                }
            }

            override fun onNewsArticleExpansion(item: NewsArticle) {
                setLikelyUrl(ProxerUrls.newsWeb(item.categoryId, item.threadId, Device.MOBILE))
            }
        }
    }

    override fun onResume() {
        super.onResume()

        isActive = true

        NotificationHelper.cancelNewsNotification(context)
    }

    override fun onPause() {
        isActive = false

        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)
    }

    override fun insert(items: List<NewsArticle>) {
        super.insert(items)

        items.firstOrNull()?.date?.let {
            StorageHelper.lastNewsDate = it
        }
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<List<NewsArticle>>().build()
    override fun constructPagedInput(page: Int) = api.notifications().news()
            .page(page)
            .limit(itemsOnPage)
            .build()
}
