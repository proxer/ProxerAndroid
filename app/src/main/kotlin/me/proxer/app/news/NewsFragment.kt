package me.proxer.app.news

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.support.v7.widget.StaggeredGridLayoutManager.VERTICAL
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.util.DeviceUtils
import me.proxer.app.view.ImageDetailActivity
import me.proxer.library.entitiy.notifications.NewsArticle
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class NewsFragment : PagedContentFragment<NewsArticle>() {

    companion object {
        fun newInstance() = NewsFragment().apply {
            arguments = bundleOf()
        }
    }

    override val emptyDataMessage = R.string.error_no_data_news

    override val viewModel: NewsViewModel by lazy {
        ViewModelProviders.of(this).get(NewsViewModel::class.java)
    }

    override val layoutManager by lazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity), VERTICAL)
    }

    override lateinit var innerAdapter: NewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = NewsAdapter(savedInstanceState, GlideApp.with(this))

        innerAdapter.clickSubject
                .bindToLifecycle(this)
                .subscribe { showPage(ProxerUrls.newsWeb(it.categoryId, it.threadId, Device.MOBILE)) }

        innerAdapter.expansionSubject
                .bindToLifecycle(this)
                .subscribe { setLikelyUrl(ProxerUrls.newsWeb(it.categoryId, it.threadId, Device.MOBILE)) }

        innerAdapter.imageClickSubject
                .bindToLifecycle(this)
                .subscribe { (view, article) ->
                    ImageDetailActivity.navigateTo(activity, ProxerUrls.newsImage(article.id, article.image), view)
                }
    }
}
