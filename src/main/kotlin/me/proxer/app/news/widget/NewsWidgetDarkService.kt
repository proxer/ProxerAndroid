package me.proxer.app.news.widget

import android.content.Intent
import android.widget.RemoteViewsService
import com.squareup.moshi.Moshi
import me.proxer.app.util.extension.getSafeStringArrayExtra
import me.proxer.app.util.extension.safeInject

/**
 * @author Ruben Gees
 */
class NewsWidgetDarkService : RemoteViewsService() {

    companion object {
        const val ARGUMENT_NEWS = "news"
    }

    private val moshi by safeInject<Moshi>()

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val news = intent.getSafeStringArrayExtra(ARGUMENT_NEWS)
            .mapNotNull { moshi.adapter(SimpleNews::class.java).fromJson(it) }

        return NewsWidgetViewsFactory(applicationContext, true, news)
    }
}
