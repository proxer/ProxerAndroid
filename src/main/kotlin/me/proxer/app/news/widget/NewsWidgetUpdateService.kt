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
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.wrapper.MaterialDrawerWrapper
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
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
        val componentName = ComponentName(applicationContext.applicationContext, NewsWidgetProvider::class.java)
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

        widgetIds.forEach { id ->
            val views = RemoteViews(applicationContext.packageName, R.layout.layout_widget_news_loading)

            bindBaseLayout(id, views)

            appWidgetManager.updateAppWidget(id, views)
        }

        disposable = api.notifications().news().buildSingle()
            .map { it.map { SimpleNews(it.id, it.threadId, it.categoryId, it.subject, it.category, it.date) } }
            .subscribeAndLogErrors({ news ->
                widgetIds.forEach { id ->
                    val views = RemoteViews(applicationContext.packageName, R.layout.layout_widget_news_list)

                    bindBaseLayout(id, views)

                    views.setRemoteAdapter(R.id.list, applicationContext.intentFor<NewsWidgetService>(
                        NewsWidgetService.ARGUMENT_NEWS_WRAPPER to bundleOf(
                            NewsWidgetService.ARGUMENT_NEWS to news.toTypedArray()
                        )
                    ))

                    appWidgetManager.updateAppWidget(id, views)
                }
            }, { error ->
                widgetIds.forEach { id ->
                    val views = RemoteViews(applicationContext.packageName, R.layout.layout_widget_news_error)
                    val action = ErrorUtils.handle(error)

                    bindBaseLayout(id, views)

                    views.setTextViewText(R.id.errorText, applicationContext.getString(action.message))

                    if (action.buttonAction == ErrorUtils.ErrorAction.ButtonAction.CAPTCHA) {
                        val errorIntent = Intent(Intent.ACTION_VIEW, ProxerUrls.captchaWeb(Device.MOBILE).androidUri())
                        val errorPendingIntent = PendingIntent.getActivity(applicationContext, 0,
                            errorIntent, FLAG_UPDATE_CURRENT)

                        views.setTextViewText(R.id.errorButton, applicationContext.getString(action.buttonMessage))
                        views.setOnClickPendingIntent(R.id.errorButton, errorPendingIntent)
                    } else {
                        views.setViewVisibility(R.id.errorButton, View.GONE)
                    }

                    appWidgetManager.updateAppWidget(id, views)
                }
            })
    }

    override fun onStopCurrentWork(): Boolean {
        disposable?.dispose()
        disposable = null

        return false
    }

    private fun bindBaseLayout(widgetId: Int, views: RemoteViews) {
        val intent = MainActivity.getSectionIntent(applicationContext, MaterialDrawerWrapper.DrawerItem.NEWS)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, FLAG_UPDATE_CURRENT)

        val detailIntent = applicationContext.intentFor<TopicActivity>()
        val detailPendingIntent = PendingIntent.getActivity(applicationContext, 0, detailIntent, FLAG_UPDATE_CURRENT)

        val updateIntent = applicationContext.intentFor<NewsWidgetProvider>()
            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))

        val updatePendingIntent = PendingIntent.getBroadcast(applicationContext, 0, updateIntent, FLAG_UPDATE_CURRENT)

        views.setOnClickPendingIntent(R.id.title, pendingIntent)
        views.setOnClickPendingIntent(R.id.refresh, updatePendingIntent)
        views.setPendingIntentTemplate(R.id.list, detailPendingIntent)

        views.setImageViewBitmap(R.id.refresh, IconicsDrawable(applicationContext, CommunityMaterial.Icon.cmd_refresh)
            .colorRes(android.R.color.white)
            .sizeDp(32)
            .paddingDp(8)
            .toBitmap())
    }
}
