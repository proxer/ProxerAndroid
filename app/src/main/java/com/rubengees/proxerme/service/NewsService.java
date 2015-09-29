package com.rubengees.proxerme.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import com.afollestad.bridge.BridgeException;
import com.rubengees.proxerme.R;
import com.rubengees.proxerme.activity.DashboardActivity;
import com.rubengees.proxerme.connection.ProxerConnection;
import com.rubengees.proxerme.entity.News;
import com.rubengees.proxerme.manager.NewsManager;

import org.json.JSONException;

import java.util.List;

import static android.support.v4.app.NotificationCompat.BigTextStyle;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class NewsService extends IntentService {

    private static final String ACTION_LOAD_NEWS = "com.rubengees.proxerme.service.action.LOAD_NEWS";
    private static final int NEWS_NOTIFICATION_ID = 1423;
    private static final int FITTING_CHARS = 35;

    public NewsService() {
        super("NewsService");
    }

    public static void startActionLoadNews(Context context) {
        Intent intent = new Intent(context, NewsService.class);
        intent.setAction(ACTION_LOAD_NEWS);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

            if (ACTION_LOAD_NEWS.equals(action)) {
                handleActionLoadNews();
            }
        }
    }

    private void handleActionLoadNews() {
        ProxerConnection.initBridge();
        NewsManager manager = NewsManager.getInstance(getApplicationContext());

        try {
            int lastId = manager.getLastId();

            if (lastId != -1) {
                List<News> news = ProxerConnection.loadNewsSync(1);
                int offset = NewsManager.calculateOffsetFromStart(news, manager.getLastId());

                manager.setLastId(news.get(0).getId());
                manager.setNewNews(offset);
                showNewsNotification(news, offset);
            }
        } catch (BridgeException | JSONException e) {
            //ignore
        }
    }

    private void showNewsNotification(List<News> news, int offset) {
        if (offset > 0 || offset == -2) {
            NotificationManager notificationManager = (NotificationManager) getApplicationContext()
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(getApplicationContext());

            builder.setAutoCancel(true).setContentTitle("News").setSmallIcon(R.mipmap.ic_launcher);

            if (offset == 1) {
                News current = news.get(0);

                if (current.getSubject().length() > FITTING_CHARS) {
                    builder.setContentText(news.get(0).getSubject().substring(0, FITTING_CHARS));
                } else {
                    builder.setContentText(current.getSubject());
                }

                builder.setStyle(new BigTextStyle(builder).bigText(current.getDescription()));
            } else {
                builder.setContentText(generateNewsNotificationAmount(offset))
                        .setStyle(new BigTextStyle(builder)
                                .bigText(generateNewsNotificationBigText(news, offset))
                                .setSummaryText(generateNewsNotificationAmount(offset)));
            }

            builder.setContentIntent(PendingIntent.getActivity(
                    this, 0, DashboardActivity.getSectionIntent(getApplicationContext(),
                            DashboardActivity.DRAWER_ID_NEWS), PendingIntent.FLAG_UPDATE_CURRENT));

            notificationManager.notify(NEWS_NOTIFICATION_ID, builder.build());
        }
    }

    private String generateNewsNotificationAmount(int offset) {
        return offset == NewsManager.OFFSET_TOO_LARGE ?
                "More than 15 News" : (offset + " " + "News");
    }

    private String generateNewsNotificationBigText(List<News> news, int offset) {
        String result = "";

        for (int i = 0; i < offset; i++) {
            if (news.get(i).getSubject().length() >= FITTING_CHARS) {
                result += news.get(i).getSubject().substring(0, FITTING_CHARS) + "...";
            } else {
                result += news.get(i).getSubject();
            }
            result += '\n';
        }

        return result;
    }
}
