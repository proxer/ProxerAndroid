package com.proxerme.app.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.SystemClock
import com.proxerme.app.receiver.ChatReceiver
import com.proxerme.app.receiver.NotificationReceiver
import com.proxerme.app.service.NotificationService
import org.jetbrains.anko.alarmManager
import org.jetbrains.anko.intentFor

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
object ServiceHelper {

    fun isNewsRetrievalEnabled(context: Context) =
            PreferenceHelper.areNewsNotificationsEnabled(context)

    fun retrieveNewsLater(context: Context) {
        cancelNewsRetrieval(context)

        if (isNewsRetrievalEnabled(context)) {
            val interval = PreferenceHelper.getNewsUpdateInterval(context) * 60L

            context.alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + interval, interval,
                    PendingIntent.getBroadcast(context, 0, context.intentFor<NotificationReceiver>()
                            .setAction(NotificationService.ACTION_LOAD_NEWS), 0))
        }
    }

    fun cancelNewsRetrieval(context: Context) {
        context.alarmManager.cancel(PendingIntent.getBroadcast(context, 0,
                context.intentFor<NotificationReceiver>()
                        .setAction(NotificationService.ACTION_LOAD_NEWS), 0))
    }

    fun retrieveChatLater(context: Context) {
        cancelChatRetrieval(context)

        val interval = StorageHelper.chatInterval * 1000L

        context.alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + interval,
                PendingIntent.getBroadcast(context, 0, context.intentFor<ChatReceiver>(), 0))
    }

    fun cancelChatRetrieval(context: Context) {
        context.alarmManager.cancel(PendingIntent.getBroadcast(context, 0,
                context.intentFor<ChatReceiver>(), 0))
    }
}