package me.proxer.app.manga.local

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import me.proxer.app.MainActivity
import me.proxer.app.R
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.NotificationUtils
import me.proxer.app.util.NotificationUtils.MANGA_CHANNEL
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.wrapper.MaterialDrawerWrapper
import me.proxer.library.api.ProxerException
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
object LocalMangaNotifications {

    private const val ID = 54354345
    private const val ERROR_ID = 479239223

    fun showOrUpdate(context: Context, maxProgress: Float, currentProgress: Float) {
        val roundedMaxProgress = Math.floor(maxProgress.toDouble()).toInt()
        val roundedCurrentProgress = Math.ceil(currentProgress.toDouble()).toInt()

        val isFinished = currentProgress >= roundedMaxProgress
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
                    .addAction(NotificationCompat.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel,
                            context.getString(R.string.notification_manga_download_cancel_action),
                            LocalMangaDownloadCancelReceiver.getPendingIntent(context)).build())
        }

        NotificationManagerCompat.from(context).notify(ID, notificationBuilder.build())
    }

    fun showError(context: Context, error: Throwable) {
        val innermostError = ErrorUtils.getInnermostError(error)
        val isIpBlockedError = innermostError is ProxerException &&
                innermostError.serverErrorType == ProxerException.ServerErrorType.IP_BLOCKED

        val intent = when {
            isIpBlockedError -> {
                PendingIntent.getActivity(context, 0, Intent(Intent.ACTION_VIEW).apply {
                    data = ProxerUrls.captchaWeb(Device.MOBILE).androidUri()
                }, 0)
            }
            else -> null
        }

        NotificationUtils.showErrorNotification(context, ERROR_ID, MANGA_CHANNEL,
                context.getString(R.string.notification_manga_download_error_title),
                context.getString(ErrorUtils.getMessage(innermostError)), intent)
    }

    fun cancelProgress(context: Context) = NotificationManagerCompat.from(context).cancel(ID)

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID)
        NotificationManagerCompat.from(context).cancel(ERROR_ID)
    }
}
