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
import me.proxer.app.util.extension.getQuantityString
import me.proxer.app.util.wrapper.MaterialDrawerWrapper.DrawerItem
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
object LocalMangaNotifications {

    private const val ID = 54354345

    fun showOrUpdate(context: Context, maxProgress: Double, currentProgress: Double, amount: Int) {
        val roundedCurrentProgress = Math.ceil(currentProgress).toInt()
        val roundedMaxProgress = Math.floor(maxProgress).toInt()
        val totalAmount = (roundedMaxProgress / 100f).toInt()

        val notificationBuilder = NotificationCompat.Builder(context, MANGA_CHANNEL)
                .setContentIntent(PendingIntent.getActivity(context, ID,
                        MainActivity.getSectionIntent(context, DrawerItem.LOCAL_MANGA),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        when (amount <= 0) {
            true -> notificationBuilder
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentTitle(context.getString(R.string.notification_manga_download_finished_title))
                    .setContentText(context.getQuantityString(R.plurals.notification_manga_download_finished_content,
                            totalAmount))
                    .setAutoCancel(true)
            false -> notificationBuilder
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle(context.getQuantityString(R.plurals.notification_manga_download_progress_title,
                            amount))
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
        val intent = when (ErrorUtils.isIpBlockedError(error)) {
            true -> PendingIntent.getActivity(context, ID, Intent(Intent.ACTION_VIEW).apply {
                data = ProxerUrls.captchaWeb(Device.MOBILE).androidUri()
            }, PendingIntent.FLAG_UPDATE_CURRENT)
            else -> null
        }

        NotificationUtils.showErrorNotification(context, ID, MANGA_CHANNEL,
                context.getString(R.string.notification_manga_download_error_title),
                context.getString(ErrorUtils.getMessage(error)), intent)
    }

    fun cancel(context: Context) = NotificationManagerCompat.from(context).cancel(ID)
}
