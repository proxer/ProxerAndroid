package me.proxer.app.anime.schedule.widget

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.JobIntentService
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import io.reactivex.disposables.Disposable
import me.proxer.app.MainActivity
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.R
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.convertToDateTime
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.wrapper.MaterialDrawerWrapper
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.intentFor
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

/**
 * @author Ruben Gees
 */
class ScheduleWidgetUpdateService : JobIntentService() {

    companion object {
        private const val JOB_ID = 31253

        private val DAY_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd. MMMM", Locale.GERMANY)

        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, ScheduleWidgetUpdateService::class.java, JOB_ID, work)
        }
    }

    private val workerThread = HandlerThread("ScheduleWidget-Worker").apply { start() }
    private val workerQueue = Handler(workerThread.looper)
    private var disposable: Disposable? = null

    override fun onHandleWork(intent: Intent) {
        val componentName = ComponentName(applicationContext, ScheduleWidgetProvider::class.java)
        val darkComponentName = ComponentName(applicationContext, ScheduleWidgetDarkProvider::class.java)

        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        val darkWidgetIds = appWidgetManager.getAppWidgetIds(darkComponentName)

        widgetIds.forEach { id -> bindLoadingLayout(appWidgetManager, id, false) }
        darkWidgetIds.forEach { id -> bindLoadingLayout(appWidgetManager, id, true) }

        disposable = api.media().calendar().buildSingle()
            .map { entries ->
                entries
                    .asSequence()
                    .filter { it.date.convertToDateTime().dayOfMonth == LocalDate.now().dayOfMonth }
                    .map { SimpleCalendarEntry(it.id, it.entryId, it.name, it.episode, it.date, it.uploadDate) }
                    .toList()
            }
            .subscribeAndLogErrors({ calendarEntries ->
                widgetIds.forEach { id -> bindListLayout(appWidgetManager, id, calendarEntries, false) }
                darkWidgetIds.forEach { id -> bindListLayout(appWidgetManager, id, calendarEntries, true) }
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

    private fun bindListLayout(
        appWidgetManager: AppWidgetManager,
        id: Int,
        calendarEntries: List<SimpleCalendarEntry>,
        dark: Boolean
    ) {
        val views = RemoteViews(
            applicationContext.packageName, when (dark) {
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

        val position = calendarEntries.indexOfFirst { it.date.convertToDateTime().isAfter(LocalDateTime.now()) }

        bindBaseLayout(id, views)

        views.setPendingIntentTemplate(R.id.list, detailPendingIntent)
        views.setRemoteAdapter(R.id.list, intent)

        appWidgetManager.updateAppWidget(id, views)

        workerQueue.postDelayed(
            {
                views.setScrollPosition(R.id.list, position)

                appWidgetManager.partiallyUpdateAppWidget(id, views)
            }, 100
        )
    }

    private fun bindErrorLayout(appWidgetManager: AppWidgetManager, id: Int, errorAction: ErrorAction, dark: Boolean) {
        val views = RemoteViews(
            applicationContext.packageName, when (dark) {
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
            applicationContext.packageName, when (dark) {
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

        views.setTextViewText(R.id.day, LocalDate.now().format(DAY_DATE_TIME_FORMATTER))
        views.setOnClickPendingIntent(R.id.title, pendingIntent)
        views.setOnClickPendingIntent(R.id.refresh, updatePendingIntent)

        views.setImageViewBitmap(
            R.id.refresh, IconicsDrawable(applicationContext, CommunityMaterial.Icon.cmd_refresh)
                .colorRes(android.R.color.white)
                .sizeDp(32)
                .paddingDp(8)
                .toBitmap()
        )
    }
}
