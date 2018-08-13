package me.proxer.app.news

import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.support.v7.widget.StaggeredGridLayoutManager.VERTICAL
import android.view.View
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.kotlin.autoDisposable
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.forum.TopicActivity
import me.proxer.app.ui.ImageDetailActivity
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.notifications.NewsArticle
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

    override val viewModel by unsafeLazy { NewsViewModelProvider.get(this) }

    override val layoutManager by unsafeLazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(requireActivity()), VERTICAL)
    }

    override var innerAdapter by Delegates.notNull<NewsAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = NewsAdapter(savedInstanceState)

        innerAdapter.clickSubject
            .autoDisposable(this.scope())
            .subscribe { TopicActivity.navigateTo(requireActivity(), it.threadId, it.categoryId, it.subject) }

        innerAdapter.imageClickSubject
            .autoDisposable(this.scope())
            .subscribe { (view, article) ->
                ImageDetailActivity.navigateTo(requireActivity(),
                    ProxerUrls.newsImage(article.id, article.image), view)
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)
    }

    override fun onResume() {
        super.onResume()

        NewsNotifications.cancel(requireContext())
    }
}
