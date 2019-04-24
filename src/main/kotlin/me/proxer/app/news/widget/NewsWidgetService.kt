package me.proxer.app.news.widget

import android.content.Intent
import android.widget.RemoteViewsService
import com.squareup.moshi.Moshi
import org.koin.android.ext.android.inject

/**
 * @author Ruben Gees
 */
class NewsWidgetService : RemoteViewsService() {

    companion object {
        const val ARGUMENT_NEWS = "news"
    }

    private val moshi by inject<Moshi>()

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val news = intent.getStringArrayExtra(ARGUMENT_NEWS)
            .mapNotNull { moshi.adapter(SimpleNews::class.java).fromJson(it) }

        return NewsWidgetViewsFactory(applicationContext, false, news)
    }
}
