package me.proxer.app.manga.local

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers

/**
 * @author Ruben Gees
 */
class LocalMangaDownloadCancelReceiver : BroadcastReceiver() {

    companion object {
        fun getPendingIntent(context: Context): PendingIntent = PendingIntent
                .getBroadcast(context, 0, Intent(context, LocalMangaDownloadCancelReceiver::class.java), 0)
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Completable
                .fromAction {
                    LocalMangaJob.cancelAll()
                    LocalMangaNotifications.cancelProgress(context)
                }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }
}
