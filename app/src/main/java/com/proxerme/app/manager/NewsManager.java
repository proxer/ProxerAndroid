package com.proxerme.app.manager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.proxerme.app.receiver.BootReceiver;
import com.proxerme.app.receiver.NewsReceiver;

/**
 * A singleton for managing the news.
 *
 * @author Ruben Gees
 */
public class NewsManager {
    private static NewsManager INSTANCE;

    private Context context;

    private String lastId;
    private int newNews = 0;

    private NewsManager(@NonNull Context context) {
        this.context = context;

        loadId();
        loadNewNews();
    }

    public static NewsManager getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            INSTANCE = new NewsManager(context);
        }
        return INSTANCE;
    }

    @Nullable
    public String getLastId() {
        return lastId;
    }

    public void setLastId(@Nullable String id) {
        lastId = id;

        saveId();
    }

    public int getNewNews() {
        return newNews;
    }

    public void setNewNews(@IntRange(from = 0) int newNews) {
        this.newNews = newNews;

        saveNewNews();
    }

    public void retrieveNewsLater() {
        cancelNewsRetrieval();
        if (isNewsRetrievalEnabled()) {
            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, NewsReceiver.class);
            PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            int interval = PreferenceManager.getUpdateInterval(context) * 60 * 1000;

            alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + interval, interval, alarmIntent);

            ComponentName receiver = new ComponentName(context, BootReceiver.class);
            PackageManager pm = context.getPackageManager();

            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }

    public void cancelNewsRetrieval() {
        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE))
                .cancel(PendingIntent.getBroadcast(context, 0,
                        new Intent(context, NewsReceiver.class), 0));
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    public boolean isNewsRetrievalEnabled() {
        return PreferenceManager.areNotificationsEnabled(context);
    }

    private void saveId() {
        StorageManager.setLastId(lastId);
    }

    private void loadId() {
        lastId = StorageManager.getLastId();
    }

    private void saveNewNews() {
        StorageManager.setNewNews(newNews);
    }

    private void loadNewNews() {
        newNews = StorageManager.getNewNews();
    }
}
