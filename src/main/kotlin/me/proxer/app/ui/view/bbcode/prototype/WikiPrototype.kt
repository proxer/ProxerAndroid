package me.proxer.app.ui.view.bbcode.prototype

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.uber.autodispose.android.ViewScopeProvider
import com.uber.autodispose.autoDisposable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.proxer.app.base.CustomTabsAware
import me.proxer.app.ui.view.ProxerWebView
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.ProxerApi
import org.koin.core.KoinComponent
import org.koin.core.get

object WikiPrototype : AutoClosingPrototype, KoinComponent {

    override val startRegex = Regex(" *wiki( .*?)?", BBPrototype.REGEX_OPTIONS)
    override val endRegex = Regex("/ *wiki *", BBPrototype.REGEX_OPTIONS)

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val link = children.firstOrNull()?.args?.text?.toString() ?: ""

        if (link.isNotBlank()) {
            @Suppress("UNCHECKED_CAST")
            val heightMap = args[ImagePrototype.HEIGHT_MAP_ARGUMENT] as MutableMap<String, Int>?
            val height = heightMap?.get(link) ?: WRAP_CONTENT

            val view = ProxerWebView(parent.context)

            view.showPageSubject
                .autoDisposable(ViewScopeProvider.from(parent))
                .subscribe { (parent.context as? CustomTabsAware)?.showPage(it) }

            view.loadingFinishedSubject
                .autoDisposable(ViewScopeProvider.from(parent))
                .subscribe { heightMap?.put(link, view.height) }

            view.layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, height)

            get<ProxerApi>().wiki.content(link).buildSingle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDisposable(ViewScopeProvider.from(parent))
                .subscribeAndLogErrors {
                    view.loadDataWithBaseURL(
                        null,
                        it.content,
                        "text/html; charset=utf-8",
                        "utf-8",
                        null
                    )
                }

            return listOf(view)
        } else {
            return emptyList()
        }
    }
}
