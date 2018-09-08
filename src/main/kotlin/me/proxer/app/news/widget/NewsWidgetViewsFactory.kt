package me.proxer.app.news.widget

import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import me.proxer.app.R
import me.proxer.app.forum.TopicActivity
import me.proxer.app.util.extension.convertToRelativeReadableTime

/**
 * @author Ruben Gees
 */
class NewsWidgetViewsFactory(
    private val context: Context,
    private val dark: Boolean,
    private val news: List<SimpleNews>
) : RemoteViewsService.RemoteViewsFactory {

    override fun hasStableIds() = true
    override fun getItemId(position: Int) = news[position].id.toLong()
    override fun getLoadingView() = null

    override fun getViewAt(position: Int): RemoteViews {
        val layout = if (dark) R.layout.layout_widget_news_dark_item else R.layout.layout_widget_news_item
        val news = news[position]

        val result = RemoteViews(context.packageName, layout)
        val topicIntent = TopicActivity.getIntent(context, news.threadId, news.categoryId, news.subject)
        val info = context.getString(
            R.string.widget_news_info,
            news.date.convertToRelativeReadableTime(context),
            news.category
        )

        result.setTextViewText(R.id.subject, news.subject)
        result.setTextViewText(R.id.info, info)
        result.setOnClickFillInIntent(R.id.container, topicIntent)

        return result
    }

    override fun getCount() = news.size
    override fun getViewTypeCount() = 1

    override fun onCreate() = Unit
    override fun onDestroy() = Unit
    override fun onDataSetChanged() = Unit
}
