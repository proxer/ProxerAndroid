package me.proxer.app.anime.schedule.widget

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.view.View
import android.widget.RemoteViews
import androidx.core.os.bundleOf
import androidx.core.os.postDelayed
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.BuildConfig
import me.proxer.app.MainActivity
import me.proxer.app.R
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.convertToDateTime
import me.proxer.app.util.extension.intentFor
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.app.util.wrapper.MaterialDrawerWrapper
import me.proxer.library.api.ProxerApi
import me.proxer.library.api.ProxerCall
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber
import java.util.Locale

/**
 * @author Ruben Gees
 */
class ScheduleWidgetUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams), KoinComponent {

    companion object {
        private const val NAME = "ScheduleWidgetUpdateWorker"

        private val dayDateTimeFormatter = DateTimeFormatter.ofPattern("dd. MMMM", Locale.GERMANY)

        fun enqueueWork() {
            val workRequest = OneTimeWorkRequestBuilder<ScheduleWidgetUpdateWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance().beginUniqueWork(NAME, ExistingWorkPolicy.REPLACE, workRequest).enqueue()
        }
    }

    private val api by inject<ProxerApi>()

    private val appWidgetManager by unsafeLazy { AppWidgetManager.getInstance(applicationContext) }

    private val widgetIds by unsafeLazy {
        appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, ScheduleWidgetProvider::class.java))
    }

    private val darkWidgetIds by unsafeLazy {
        appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, ScheduleWidgetDarkProvider::class.java))
    }

    private val workerThread = HandlerThread("ScheduleWidget-Worker").apply { start() }
    private val workerQueue = Handler(workerThread.looper)

    private var currentCall: ProxerCall<*>? = null

    override fun doWork(): Result {
        widgetIds.forEach { id -> bindLoadingLayout(appWidgetManager, id, false) }
        darkWidgetIds.forEach { id -> bindLoadingLayout(appWidgetManager, id, true) }

        return try {
            val calendarEntries = if (!isStopped) {
                api.media().calendar()
                    .build()
                    .also { currentCall = it }
                    .safeExecute()
                    .asSequence()
                    .filter { it.date.convertToDateTime().dayOfMonth == LocalDate.now().dayOfMonth }
                    .map { SimpleCalendarEntry(it.id, it.entryId, it.name, it.episode, it.date, it.uploadDate) }
                    .toList()
            } else {
                emptyList()
            }

            if (!isStopped) {
                widgetIds.forEach { id -> bindListLayout(appWidgetManager, id, calendarEntries, false) }
                darkWidgetIds.forEach { id -> bindListLayout(appWidgetManager, id, calendarEntries, true) }
            }

            Result.SUCCESS
        } catch (error: Throwable) {
            Timber.e(error)

            if (!isStopped) {
                val action = ErrorUtils.handle(error)

                widgetIds.forEach { id -> bindErrorLayout(appWidgetManager, id, action, false) }
                darkWidgetIds.forEach { id -> bindErrorLayout(appWidgetManager, id, action, true) }
            }

            return Result.FAILURE
        }
    }

    override fun onStopped(cancelled: Boolean) {
        currentCall?.cancel()
        currentCall = null
    }

    private fun bindListLayout(
        appWidgetManager: AppWidgetManager,
        id: Int,
        calendarEntries: List<SimpleCalendarEntry>,
        dark: Boolean
    ) {
        val views = RemoteViews(
            BuildConfig.APPLICATION_ID, when (dark) {
                true -> R.layout.layout_widget_schedule_dark_list
                false -> R.layout.layout_widget_schedule_list
            }
        )

        val params = arrayOf(
            ScheduleWidgetService.ARGUMENT_CALENDAR_ENTRIES_WRAPPER to bundleOf(
                ScheduleWidgetService.ARGUMENT_CALENDAR_ENTRIES to calendarEntries.toTypedArray()
            )
        )

        val intent = when (dark) {
            true -> applicationContext.intentFor<ScheduleWidgetDarkService>(*params)
            false -> applicationContext.intentFor<ScheduleWidgetService>(*params)
        }

        val detailIntent = applicationContext.intentFor<MediaActivity>()
        val detailPendingIntent = PendingIntent.getActivity(applicationContext, 0, detailIntent, FLAG_UPDATE_CURRENT)

        val position = calendarEntries
            .indexOfFirst { it.date.convertToDateTime().isAfter(LocalDateTime.now()) }
            .let {
                when (it < 0) {
                    true -> when (LocalDateTime.now().isAfter(calendarEntries.last().date.convertToDateTime())) {
                        true -> calendarEntries.lastIndex
                        false -> 0
                    }
                    false -> it
                }
            }

        bindBaseLayout(id, views)

        views.setPendingIntentTemplate(R.id.list, detailPendingIntent)
        views.setRemoteAdapter(R.id.list, intent)

        appWidgetManager.updateAppWidget(id, views)

        workerQueue.postDelayed(100) {
            views.setScrollPosition(R.id.list, position)

            appWidgetManager.partiallyUpdateAppWidget(id, views)
        }
    }

    private fun bindErrorLayout(appWidgetManager: AppWidgetManager, id: Int, errorAction: ErrorAction, dark: Boolean) {
        val views = RemoteViews(
            BuildConfig.APPLICATION_ID, when (dark) {
                true -> R.layout.layout_widget_schedule_dark_error
                false -> R.layout.layout_widget_schedule_error
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
            BuildConfig.APPLICATION_ID, when (dark) {
                true -> R.layout.layout_widget_schedule_dark_loading
                false -> R.layout.layout_widget_schedule_loading
            }
        )

        bindBaseLayout(id, views)

        appWidgetManager.updateAppWidget(id, views)
    }

    private fun bindBaseLayout(id: Int, views: RemoteViews) {
        val intent = MainActivity.getSectionIntent(applicationContext, MaterialDrawerWrapper.DrawerItem.SCHEDULE)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, FLAG_UPDATE_CURRENT)

        val updateIntent = applicationContext.intentFor<ScheduleWidgetProvider>()
            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(id))

        val updatePendingIntent = PendingIntent.getBroadcast(applicationContext, 0, updateIntent, FLAG_UPDATE_CURRENT)

        views.setTextViewText(R.id.day, LocalDate.now().format(dayDateTimeFormatter))
        views.setOnClickPendingIntent(R.id.title, pendingIntent)
        views.setOnClickPendingIntent(R.id.refresh, updatePendingIntent)

        views.setImageViewBitmap(
            R.id.refresh, IconicsDrawable(applicationContext, CommunityMaterial.Icon2.cmd_refresh)
                .colorRes(android.R.color.white)
                .sizeDp(32)
                .paddingDp(8)
                .toBitmap()
        )
    }
}
