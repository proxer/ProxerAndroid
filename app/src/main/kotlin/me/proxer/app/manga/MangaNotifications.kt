package me.proxer.app.manga

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationManagerCompat
import me.proxer.app.R
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.NotificationUtils
import me.proxer.app.util.NotificationUtils.MANGA_CHANNEL
import me.proxer.app.util.extension.androidUri
import me.proxer.library.api.ProxerException
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
object MangaNotifications {

    private const val ERROR_ID = 479239223

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

    fun cancel(context: Context) = NotificationManagerCompat.from(context).cancel(ERROR_ID)
}
