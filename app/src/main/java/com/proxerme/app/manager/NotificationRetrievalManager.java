package com.proxerme.app.manager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.proxerme.app.receiver.MessagesReceiver;
import com.proxerme.app.receiver.NewsReceiver;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class NotificationRetrievalManager {

    /**
     * Retrieves News and interprets them in a background Service in the time span, specified in the
     * settings. If a news retrieval was already queued, it will be cancelled.
     *
     * @param context The context.
     */
    public static void retrieveNewsLater(@NonNull Context context) {
        cancelNewsRetrieval(context);
        if (isNewsRetrievalEnabled(context)) {
            int interval = PreferenceManager.getNewsUpdateInterval(context) * 60 * 1000;

            retrieveLater(context, NewsReceiver.class, interval);
        }
    }

    /**
     * Cancels a queued news retrieval. If there is none, nothing will happen.
     *
     * @param context The context.
     */
    public static void cancelNewsRetrieval(@NonNull Context context) {
        cancelRetrieval(context, NewsReceiver.class);
    }

    /**
     * Returns if news retrieval is enabled.
     *
     * @param context The context.
     * @return True, if news retrieval is enabled.
     */
    public static boolean isNewsRetrievalEnabled(@NonNull Context context) {
        return PreferenceManager.areNewsNotificationsEnabled(context);
    }

    /**
     * Retrieves new messages and interprets them in a background Service.
     * If a messages retrieval was already queued, it will be cancelled.
     * The interval is retrieved from the storage and will be incremented in the service.
     *
     * @param context The context.
     */
    public static void retrieveMessagesLater(@NonNull Context context) {
        cancelMessagesRetrieval(context);
        if (isMessagesRetrievalEnabled(context)) {
            int interval = StorageManager.getMessagesInterval() * 1000;

            retrieveLater(context, MessagesReceiver.class, interval);
        }
    }

    /**
     * Cancels a queued messages retrieval. If there is none, nothing will happen.
     *
     * @param context The context.
     */
    public static void cancelMessagesRetrieval(@NonNull Context context) {
        cancelRetrieval(context, MessagesReceiver.class);
    }

    /**
     * Returns if messages retrieval is enabled.
     *
     * @param context The context.
     * @return True, if messages retrieval is enabled.
     */
    public static boolean isMessagesRetrievalEnabled(@NonNull Context context) {
        return PreferenceManager.areMessagesNotificationsEnabled(context);
    }

    private static void retrieveLater(@NonNull Context context, @NonNull Class<?> receiver,
                                      int interval) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, receiver);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + interval, interval, alarmIntent);

        ComponentName receiverName = new ComponentName(context, receiver);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiverName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    private static void cancelRetrieval(@NonNull Context context, @NonNull Class<?> receiver) {
        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE))
                .cancel(PendingIntent.getBroadcast(context, 0,
                        new Intent(context, receiver), 0));
        ComponentName receiverName = new ComponentName(context, receiver);
        PackageManager pm = context.getPackageManager();

        if (!isNewsRetrievalEnabled(context) && !isMessagesRetrievalEnabled(context)) {
            pm.setComponentEnabledSetting(receiverName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }

}
