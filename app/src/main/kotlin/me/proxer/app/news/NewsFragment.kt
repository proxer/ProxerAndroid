package me.proxer.app.news

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import me.proxer.app.GlideApp
import me.proxer.app.base.PagedContentFragment
import me.proxer.library.entitiy.notifications.NewsArticle
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class NewsFragment : PagedContentFragment<NewsArticle>() {

    companion object {
        fun newInstance() = NewsFragment().apply {
            arguments = Bundle()
        }
    }

    override val viewModel by lazy { ViewModelProviders.of(this).get(NewsViewModel::class.java) }
    override val layoutManager by lazy { LinearLayoutManager(context) }
    override lateinit var innerAdapter: NewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = NewsAdapter(savedInstanceState, GlideApp.with(this))
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.clickSubject
                .bindToLifecycle(this)
                .subscribe { showPage(ProxerUrls.newsWeb(it.categoryId, it.threadId, Device.MOBILE)) }

        innerAdapter.expansionSubject
                .bindToLifecycle(this)
                .subscribe { setLikelyUrl(ProxerUrls.newsWeb(it.categoryId, it.threadId, Device.MOBILE)) }

        innerAdapter.imageClickSubject
                .bindToLifecycle(this)
                .subscribe { Snackbar.make(root, "Image Clicked! ${it.second.subject}", Snackbar.LENGTH_SHORT).show() }
    }
}
