package me.proxer.app.manga.local

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import me.proxer.app.MainActivity
import me.proxer.app.R
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.NotificationUtils
import me.proxer.app.util.NotificationUtils.MANGA_CHANNEL
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.wrapper.MaterialDrawerWrapper
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
object LocalMangaNotifications {

    private const val ID = 54354345

    fun showOrUpdate(context: Context, maxProgress: Double, currentProgress: Double) {
        val roundedMaxProgress = Math.floor(maxProgress).toInt()
        val roundedCurrentProgress = Math.ceil(currentProgress).toInt()
        Log.i("Progress", "max: $roundedMaxProgress, current: $roundedCurrentProgress")

        val isFinished = roundedCurrentProgress >= roundedMaxProgress
        val notificationBuilder = NotificationCompat.Builder(context, MANGA_CHANNEL)
                .setContentTitle(context.getString(R.string.notification_manga_download_progress_title))
                .setContentIntent(PendingIntent.getActivity(context, 0,
                        MainActivity.getSectionIntent(context, MaterialDrawerWrapper.DrawerItem.LOCAL_MANGA),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        when (isFinished) {
            true -> notificationBuilder
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentText(context.getString(R.string.notification_manga_download_finished_content))
                    .setAutoCancel(true)
            false -> notificationBuilder
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setProgress(roundedMaxProgress, roundedCurrentProgress, false)
                    .setSubText("${(currentProgress / maxProgress * 100).toInt()}%")
                    .setOngoing(true)
                    .addAction(NotificationCompat.Action.Builder(R.drawable.ic_stat_cancel,
                            context.getString(R.string.notification_manga_download_cancel_action),
                            LocalMangaDownloadCancelReceiver.getPendingIntent(context)).build())
        }

        NotificationManagerCompat.from(context).notify(ID, notificationBuilder.build())
    }

    fun showError(context: Context, error: Throwable) {
        val intent = if (ErrorUtils.isIpBlockedError(error)) {
            PendingIntent.getActivity(context, 0, Intent(Intent.ACTION_VIEW).apply {
                data = ProxerUrls.captchaWeb(Device.MOBILE).androidUri()
            }, PendingIntent.FLAG_UPDATE_CURRENT)
        } else {
            null
        }

        NotificationUtils.showErrorNotification(context, ID, MANGA_CHANNEL,
                context.getString(R.string.notification_manga_download_error_title),
                context.getString(ErrorUtils.getMessage(error)), intent)
    }

    fun cancel(context: Context) = NotificationManagerCompat.from(context).cancel(ID)
}
