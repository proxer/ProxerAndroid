package me.proxer.app.fragment.news

import android.os.Bundle
import android.widget.ImageView
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.ImageDetailActivity
import me.proxer.app.adapter.news.NewsArticleAdapter
import me.proxer.app.adapter.news.NewsArticleAdapter.NewsAdapterCallback
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.PagedLoadingFragment
import me.proxer.app.task.asyncProxerTask
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
        fun newInstance(): NewsArticleFragment {
            return NewsArticleFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    override val isSwipeToRefreshEnabled = true
    override val itemsOnPage = 15
    override val emptyResultMessage = R.string.error_no_data_news

    override val innerAdapter = NewsArticleAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter.callback = object : NewsAdapterCallback {
            override fun onNewsArticleClick(item: NewsArticle) {
                showPage(ProxerUrls.newsWeb(item.categoryId, item.threadId, Device.MOBILE))
            }

            override fun onNewsArticleImageClick(view: ImageView, item: NewsArticle) {
                if (view.drawable != null) {
                    ImageDetailActivity.navigateTo(activity, view, ProxerUrls.newsImage(item.id, item.image))
                }
            }

            override fun onNewsArticleExpansion(item: NewsArticle) {
                setLikelyUrl(ProxerUrls.newsWeb(item.categoryId, item.threadId, Device.MOBILE))
            }
        }
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<List<NewsArticle>>().build()
    override fun constructPagedInput(page: Int) = api.notifications().news()
            .page(page)
            .limit(itemsOnPage)
            .build()
}
