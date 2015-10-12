package com.proxerme.app.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.proxerme.app.manager.NewsManager;
import com.proxerme.app.manager.NotificationManager;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerException;
import com.proxerme.library.entity.News;

import java.util.List;

/**
 * An {@link IntentService}, which retrieves the News and shows a notification,if there are unread
 * ones.
 *
 * @author Ruben Gees
 */
public class NewsService extends IntentService {

    private static final String ACTION_LOAD_NEWS = "com.proxerme.app.service.action.LOAD_NEWS";

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
        ProxerConnection.init();
        NewsManager manager = NewsManager.getInstance(this);

        try {
            String lastId = manager.getLastId();

            if (lastId != null) {
                List<News> news = ProxerConnection.loadNews(1).executeSynchronized();
                int offset = NewsManager.calculateOffsetFromStart(news, manager.getLastId());

                manager.setLastId(news.get(0).getId());
                manager.setNewNews(offset);
                NotificationManager.showNewsNotification(this, news, offset);
            }
        } catch (ProxerException e) {
            //ignore
        }
    }

}
