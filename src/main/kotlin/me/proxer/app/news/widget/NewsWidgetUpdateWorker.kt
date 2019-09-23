package me.proxer.app.news.widget

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorRes
import com.mikepenz.iconics.utils.paddingDp
import com.mikepenz.iconics.utils.sizeDp
import com.squareup.moshi.Moshi
import me.proxer.app.BuildConfig
import me.proxer.app.MainActivity
import me.proxer.app.R
import me.proxer.app.forum.TopicActivity
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.intentFor
import me.proxer.app.util.extension.safeInject
import me.proxer.app.util.extension.toInstantBP
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.app.util.wrapper.MaterialDrawerWrapper
import me.proxer.library.ProxerApi
import me.proxer.library.ProxerCall
import org.koin.core.KoinComponent
import timber.log.Timber

/**
 * @author Ruben Gees
 */
class NewsWidgetUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams), KoinComponent {

    companion object : KoinComponent {
        private const val NAME = "NewsWidgetUpdateWorker"

        private val workManager by safeInject<WorkManager>()

        fun enqueueWork() {
            val workRequest = OneTimeWorkRequestBuilder<NewsWidgetUpdateWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            workManager.beginUniqueWork(NAME, ExistingWorkPolicy.REPLACE, workRequest).enqueue()
        }
    }

    private val api by safeInject<ProxerApi>()
    private val moshi by safeInject<Moshi>()

    private val appWidgetManager by unsafeLazy { AppWidgetManager.getInstance(applicationContext) }

    private val widgetIds by unsafeLazy {
        appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, NewsWidgetProvider::class.java))
    }

    private val darkWidgetIds by unsafeLazy {
        appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, NewsWidgetDarkProvider::class.java))
    }

    private var currentCall: ProxerCall<*>? = null

    override fun doWork(): Result {
        widgetIds.forEach { id -> bindLoadingLayout(appWidgetManager, id, false) }
        darkWidgetIds.forEach { id -> bindLoadingLayout(appWidgetManager, id, true) }

        return try {
            val news = if (!isStopped) {
                api.notifications.news()
                    .build()
                    .also { currentCall = it }
                    .safeExecute()
                    .map {
                        SimpleNews(it.id, it.threadId, it.categoryId, it.subject, it.category, it.date.toInstantBP())
                    }
            } else {
                emptyList()
            }

            if (!isStopped) {
                if (news.isEmpty()) {
                    val noDataAction = ErrorAction(R.string.error_no_data_news)

                    widgetIds.forEach { id -> bindErrorLayout(appWidgetManager, id, noDataAction, false) }
                    darkWidgetIds.forEach { id -> bindErrorLayout(appWidgetManager, id, noDataAction, true) }
                } else {
                    widgetIds.forEach { id -> bindListLayout(appWidgetManager, id, news, false) }
                    darkWidgetIds.forEach { id -> bindListLayout(appWidgetManager, id, news, true) }
                }
            }

            Result.success()
        } catch (error: Throwable) {
            Timber.e(error)

            if (!isStopped) {
                val action = ErrorUtils.handle(error)

                widgetIds.forEach { id -> bindErrorLayout(appWidgetManager, id, action, false) }
                darkWidgetIds.forEach { id -> bindErrorLayout(appWidgetManager, id, action, true) }
            }

            return Result.failure()
        }
    }

    override fun onStopped() {
        currentCall?.cancel()
        currentCall = null
    }

    private fun bindListLayout(appWidgetManager: AppWidgetManager, id: Int, news: List<SimpleNews>, dark: Boolean) {
        val views = RemoteViews(
            BuildConfig.APPLICATION_ID,
            when (dark) {
                true -> R.layout.layout_widget_news_dark_list
                false -> R.layout.layout_widget_news_list
            }
        )

        val serializedNews = news
            .map { moshi.adapter(SimpleNews::class.java).toJson(it) }
            .toTypedArray()

        val intent = when (dark) {
            true -> applicationContext.intentFor<NewsWidgetDarkService>(
                NewsWidgetDarkService.ARGUMENT_NEWS to serializedNews
            )
            false -> applicationContext.intentFor<NewsWidgetService>(
                NewsWidgetService.ARGUMENT_NEWS to serializedNews
            )
        }

        val detailIntent = applicationContext.intentFor<TopicActivity>()
        val detailPendingIntent = PendingIntent.getActivity(applicationContext, 0, detailIntent, FLAG_UPDATE_CURRENT)

        bindBaseLayout(id, views)

        views.setPendingIntentTemplate(R.id.list, detailPendingIntent)
        views.setRemoteAdapter(R.id.list, intent)

        appWidgetManager.updateAppWidget(id, views)
    }

    private fun bindErrorLayout(appWidgetManager: AppWidgetManager, id: Int, errorAction: ErrorAction, dark: Boolean) {
        val views = RemoteViews(
            BuildConfig.APPLICATION_ID,
            when (dark) {
                true -> R.layout.layout_widget_news_dark_error
                false -> R.layout.layout_widget_news_error
            }
        )

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
        val views = RemoteViews(
            BuildConfig.APPLICATION_ID,
            when (dark) {
                true -> R.layout.layout_widget_news_dark_loading
                false -> R.layout.layout_widget_news_loading
            }
        )

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

        views.setImageViewBitmap(
            R.id.refresh,
            IconicsDrawable(applicationContext, CommunityMaterial.Icon2.cmd_refresh)
                .colorRes(android.R.color.white)
                .sizeDp(32)
                .paddingDp(8)
                .toBitmap()
        )
    }
}
