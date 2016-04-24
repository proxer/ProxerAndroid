package com.proxerme.app.manager;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.proxerme.app.event.SectionChangedEvent;
import com.proxerme.app.receiver.BootReceiver;
import com.proxerme.app.receiver.NotificationReceiver;
import com.proxerme.app.service.NotificationService;
import com.proxerme.app.util.helper.NotificationHelper;
import com.proxerme.app.util.helper.PreferenceHelper;
import com.proxerme.app.util.helper.StorageHelper;
import com.proxerme.library.event.success.LoginEvent;
import com.proxerme.library.event.success.LogoutEvent;

import org.greenrobot.eventbus.Subscribe;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class NotificationManager extends Manager {

    private Context context;

    public NotificationManager(@NonNull Context context) {
        super();

        this.context = context;
    }

    /**
     * Returns if news retrieval is enabled.
     *
     * @param context The context.
     * @return True, if news retrieval is enabled.
     */
    public static boolean isNewsRetrievalEnabled(@NonNull Context context) {
        return PreferenceHelper.areNewsNotificationsEnabled(context);
    }

    /**
     * Retrieves News and interprets them in a background Service in the time span, specified in the
     * settings. If a news retrieval was already queued, it will be cancelled.
     *
     * @param context The context.
     */
    public void retrieveNewsLater(@NonNull Context context) {
        cancelNewsRetrieval(context);
        if (isNewsRetrievalEnabled(context)) {
            int interval = PreferenceHelper.getNewsUpdateInterval(context) * 60 * 1000;

            retrieveLater(context, NotificationService.ACTION_LOAD_NEWS, interval);
        }
    }

    /**
     * Cancels a queued news retrieval. If there is none, nothing will happen.
     *
     * @param context The context.
     */
    public void cancelNewsRetrieval(@NonNull Context context) {
        cancelRetrieval(context, NotificationService.ACTION_LOAD_NEWS);
    }

    /**
     * Retrieves new messages and interprets them in a background Service.
     * If a messages retrieval was already queued, it will be cancelled.
     * The interval is retrieved from the storage and will be incremented in the service.
     *
     * @param context The context.
     */
    public void retrieveConferencesLater(@NonNull Context context) {
        cancelMessagesRetrieval(context);
        if (isMessagesRetrievalEnabled(context)) {
            int interval = StorageHelper.getMessagesInterval() * 1000;

            retrieveLater(context, NotificationService.ACTION_LOAD_CONFERENCES, interval);
        }
    }

    /**
     * Cancels a queued messages retrieval. If there is none, nothing will happen.
     *
     * @param context The context.
     */
    public void cancelMessagesRetrieval(@NonNull Context context) {
        cancelRetrieval(context, NotificationService.ACTION_LOAD_CONFERENCES);
    }

    /**
     * Returns if messages retrieval is enabled.
     *
     * @param context The context.
     * @return True, if messages retrieval is enabled.
     */
    public boolean isMessagesRetrievalEnabled(@NonNull Context context) {
        return PreferenceHelper.areMessagesNotificationsEnabled(context);
    }

    private void retrieveLater(@NonNull Context context,
                               @NonNull @NotificationService.NotificationAction String
                                              action, int interval) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);

        intent.setAction(action);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + interval, interval, alarmIntent);

        ComponentName receiverName = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiverName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    private void cancelRetrieval(@NonNull Context context,
                                 @NonNull @NotificationService.NotificationAction String
                                                action) {
        Intent intent = new Intent(context, NotificationReceiver.class);

        intent.setAction(action);
        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE))
                .cancel(PendingIntent.getBroadcast(context, 0, intent, 0));
        ComponentName receiverName = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        if (getOngoingRetrievals(context) > 0) {
            pm.setComponentEnabledSetting(receiverName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }

    private int getOngoingRetrievals(@NonNull Context context) {
        int count = 0;

        if (isNewsRetrievalEnabled(context)) {
            count++;
        }

        if (isMessagesRetrievalEnabled(context)) {
            count++;
        }

        return count;
    }

    @Override
    public void destroy() {
        super.destroy();

        this.context = null;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        retrieveConferencesLater(context);
    }

    @Subscribe
    public void onLogout(LogoutEvent event) {
        cancelMessagesRetrieval(context);
    }

    @SuppressLint("SwitchIntDef")
    @Subscribe
    public void onSectionChanged(SectionChangedEvent event) {
        switch (event.getNewSection()) {
            case com.proxerme.app.util.Section.CONFERENCES:
                NotificationHelper.cancel(context, NotificationHelper.MESSAGES_NOTIFICATION);

                break;
            case com.proxerme.app.util.Section.MESSAGES:
                NotificationHelper.cancel(context, NotificationHelper.MESSAGES_NOTIFICATION);

                break;
            case com.proxerme.app.util.Section.NEWS:
                NotificationHelper.cancel(context, NotificationHelper.NEWS_NOTIFICATION);

                break;
            case com.proxerme.app.util.Section.SETTINGS:
                break;
        }
    }

}
