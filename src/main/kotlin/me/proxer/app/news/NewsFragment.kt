package me.proxer.app.news

import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.support.v7.widget.StaggeredGridLayoutManager.VERTICAL
import android.view.View
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.ui.ImageDetailActivity
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.notifications.NewsArticle
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import org.jetbrains.anko.bundleOf
import kotlin.properties.Delegates

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

    override val viewModel: NewsViewModel by unsafeLazy { NewsViewModelProvider.get(this) }

    override val layoutManager by unsafeLazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity), VERTICAL)
    }

    override var innerAdapter by Delegates.notNull<NewsAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = NewsAdapter(savedInstanceState)

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

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)
    }

    override fun onResume() {
        super.onResume()

        NewsNotifications.cancel(context)
    }
}
