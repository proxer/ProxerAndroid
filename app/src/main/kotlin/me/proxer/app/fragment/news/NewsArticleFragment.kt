package me.proxer.app.fragment.news

import android.os.Bundle
import android.widget.ImageView
import com.proxerme.library.api.ProxerCall
import com.proxerme.library.entitiy.notifications.NewsArticle
import com.proxerme.library.enums.Device
import com.proxerme.library.util.ProxerUrls
import me.proxer.app.activity.ImageDetailActivity
import me.proxer.app.adapter.news.NewsArticleAdapter
import me.proxer.app.adapter.news.NewsArticleAdapter.NewsAdapterCallback
import me.proxer.app.fragment.base.PagedLoadingFragment
import me.proxer.app.task.ProxerTask
import me.proxer.app.util.extension.api

/**
 * @author Ruben Gees
 */
class NewsArticleFragment : PagedLoadingFragment<ProxerCall<List<NewsArticle>>, NewsArticle>() {

    companion object {
        fun newInstance(): NewsArticleFragment {
            return NewsArticleFragment()
        }
    }

    override val isSwipeToRefreshEnabled = true
    override val itemsOnPage = 15

    override val innerAdapter = NewsArticleAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter.callback = object : NewsAdapterCallback {
            override fun onNewsArticleClick(item: NewsArticle) {
                showPage(ProxerUrls.newsWeb(item.categoryId, item.threadId, Device.MOBILE))
            }

            override fun onNewsArticleImageClick(view: ImageView, item: NewsArticle) {
                ImageDetailActivity.navigateTo(activity, view, ProxerUrls.newsImage(item.id, item.image))
            }

            override fun onNewsArticleExpansion(item: NewsArticle) {
                setLikelyUrl(ProxerUrls.newsWeb(item.categoryId, item.threadId, Device.MOBILE))
            }
        }
    }

    override fun constructTask() = ProxerTask<List<NewsArticle>>()
    override fun constructPagedInput(page: Int): ProxerCall<List<NewsArticle>> {
        return api.notifications().news()
                .page(page)
                .limit(itemsOnPage)
                .build()
    }
}
