package me.proxer.app.news.widget

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.support.v4.app.JobIntentService
import android.view.View
import android.widget.RemoteViews
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import io.reactivex.disposables.Disposable
import me.proxer.app.MainActivity
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.R
import me.proxer.app.forum.TopicActivity
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.wrapper.MaterialDrawerWrapper
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.intentFor

/**
 * @author Ruben Gees
 */
class NewsWidgetUpdateService : JobIntentService() {

    companion object {
        private const val JOB_ID = 31221

        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, NewsWidgetUpdateService::class.java, JOB_ID, work)
        }
    }

    private var disposable: Disposable? = null

    override fun onHandleWork(intent: Intent) {
        val componentName = ComponentName(applicationContext, NewsWidgetProvider::class.java)
        val darkComponentName = ComponentName(applicationContext, NewsWidgetDarkProvider::class.java)

        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        val darkWidgetIds = appWidgetManager.getAppWidgetIds(darkComponentName)

        widgetIds.forEach { id -> bindLoadingLayout(appWidgetManager, id, false) }
        darkWidgetIds.forEach { id -> bindLoadingLayout(appWidgetManager, id, true) }

        disposable = api.notifications().news().buildSingle()
            .map { news ->
                news.map {
                    SimpleNews(
                        it.id,
                        it.threadId,
                        it.categoryId,
                        it.subject,
                        it.category,
                        it.date
                    )
                }
            }
            .subscribeAndLogErrors({ news ->
                widgetIds.forEach { id -> bindListLayout(appWidgetManager, id, news, false) }
                darkWidgetIds.forEach { id -> bindListLayout(appWidgetManager, id, news, true) }
            }, { error ->
                val action = ErrorUtils.handle(error)

                widgetIds.forEach { id -> bindErrorLayout(appWidgetManager, id, action, false) }
                darkWidgetIds.forEach { id -> bindErrorLayout(appWidgetManager, id, action, true) }
            })
    }

    override fun onStopCurrentWork(): Boolean {
        disposable?.dispose()
        disposable = null

        return false
    }

    private fun bindListLayout(appWidgetManager: AppWidgetManager, id: Int, news: List<SimpleNews>, dark: Boolean) {
        val views = RemoteViews(applicationContext.packageName, when (dark) {
            true -> R.layout.layout_widget_news_dark_list
            false -> R.layout.layout_widget_news_list
        })

        val params = arrayOf(
            NewsWidgetService.ARGUMENT_NEWS_WRAPPER to bundleOf(
                NewsWidgetService.ARGUMENT_NEWS to news.toTypedArray()
            )
        )

        val intent = when (dark) {
            true -> applicationContext.intentFor<NewsWidgetDarkService>(*params)
            false -> applicationContext.intentFor<NewsWidgetService>(*params)
        }

        val detailIntent = applicationContext.intentFor<TopicActivity>()
        val detailPendingIntent = PendingIntent.getActivity(applicationContext, 0, detailIntent, FLAG_UPDATE_CURRENT)

        bindBaseLayout(id, views)

        views.setPendingIntentTemplate(R.id.list, detailPendingIntent)
        views.setRemoteAdapter(R.id.list, intent)

        appWidgetManager.updateAppWidget(id, views)
    }

    private fun bindErrorLayout(appWidgetManager: AppWidgetManager, id: Int, errorAction: ErrorAction, dark: Boolean) {
        val views = RemoteViews(applicationContext.packageName, when (dark) {
            true -> R.layout.layout_widget_news_dark_error
            false -> R.layout.layout_widget_news_error
        })

        val errorIntent = errorAction.toIntent()

        bindBaseLayout(id, views)

        views.setTextViewText(R.id.errorText, applicationContext.getString(errorAction.message))

        if (errorIntent != null) {
            val errorPendingIntent = PendingIntent.getActivity(applicationContext, 0, errorIntent, FLAG_UPDATE_CURRENT)

            views.setTextViewText(R.id.errorButton, applicationContext.getString(errorAction.buttonMessage))
            views.setOnClickPendingIntent(R.id.errorButton, errorPendingIntent)
        } else {
            views.setViewVisibility(R.id.errorButton, View.GONE)
        }

        appWidgetManager.updateAppWidget(id, views)
    }

    private fun bindLoadingLayout(appWidgetManager: AppWidgetManager, id: Int, dark: Boolean) {
        val views = RemoteViews(applicationContext.packageName, when (dark) {
            true -> R.layout.layout_widget_news_dark_loading
            false -> R.layout.layout_widget_news_loading
        })

        bindBaseLayout(id, views)

        appWidgetManager.updateAppWidget(id, views)
    }

    private fun bindBaseLayout(id: Int, views: RemoteViews) {
        val intent = MainActivity.getSectionIntent(applicationContext, MaterialDrawerWrapper.DrawerItem.NEWS)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, FLAG_UPDATE_CURRENT)

        val updateIntent = applicationContext.intentFor<NewsWidgetProvider>()
            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(id))

        val updatePendingIntent = PendingIntent.getBroadcast(applicationContext, 0, updateIntent, FLAG_UPDATE_CURRENT)

        views.setOnClickPendingIntent(R.id.title, pendingIntent)
        views.setOnClickPendingIntent(R.id.refresh, updatePendingIntent)

        views.setImageViewBitmap(R.id.refresh, IconicsDrawable(applicationContext, CommunityMaterial.Icon.cmd_refresh)
            .colorRes(android.R.color.white)
            .sizeDp(32)
            .paddingDp(8)
            .toBitmap())
    }
}
