package com.proxerme.app.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.proxerme.app.application.MainApplication;
import com.proxerme.app.manager.NewsManager;
import com.proxerme.app.manager.NotificationManager;
import com.proxerme.app.manager.NotificationRetrievalManager;
import com.proxerme.app.manager.StorageManager;
import com.proxerme.app.util.PagingHelper;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerException;
import com.proxerme.library.entity.Conference;
import com.proxerme.library.entity.LoginUser;
import com.proxerme.library.entity.News;
import com.proxerme.library.util.ProxerInfo;

import java.util.List;

/**
 * An IntentService, which retrieves the News and shows a notification if there are unread
 * ones.
 *
 * @author Ruben Gees
 */
public class NotificationService extends IntentService {

    private static final String ACTION_LOAD_NEWS = "com.proxerme.app.service.action.LOAD_NEWS";
    private static final String ACTION_LOAD_MESSAGES = "com.proxerme.app.service.action.LOAD_MESSAGES";

    public NotificationService() {
        super("NotificationService");
    }

    public static void startActionLoadNews(@NonNull Context context) {
        Intent intent = new Intent(context, NotificationService.class);
        intent.setAction(ACTION_LOAD_NEWS);
        context.startService(intent);
    }

    public static void startActionLoadMessages(@NonNull Context context) {
        Intent intent = new Intent(context, NotificationService.class);
        intent.setAction(ACTION_LOAD_MESSAGES);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null && !MainApplication.isVisible) {
            final String action = intent.getAction();

            if (ACTION_LOAD_NEWS.equals(action)) {
                handleActionLoadNews();
            } else if (ACTION_LOAD_MESSAGES.equals(action)) {
                handleActionLoadMessages();
            }
        }
    }

    private void handleActionLoadNews() {
        NewsManager manager = NewsManager.getInstance();

        try {
            String lastId = manager.getLastId();

            if (lastId != null) {
                List<News> news = ProxerConnection.loadNews(1).executeSynchronized();
                int offset = PagingHelper.calculateOffsetFromStart(news, manager.getLastId(),
                        ProxerInfo.NEWS_ON_PAGE);

                manager.setNewNews(offset);
                NotificationManager.showNewsNotification(this, news, offset);
            }
        } catch (ProxerException ignored) {

        }
    }

    private void handleActionLoadMessages() {
        LoginUser user = StorageManager.getUser();

        if (user != null) {
            try {
                ProxerConnection.login(user).executeSynchronized();
                List<Conference> conferences = ProxerConnection.loadConferences(1).executeSynchronized();

                for (int i = 0; i < conferences.size(); i++) {
                    if (conferences.get(i).isRead()) {
                        conferences = conferences.subList(0, i);

                        break;
                    }
                }

                NotificationManager.showMessagesNotification(this, conferences);
                StorageManager.setNewMessages(conferences.size());
            } catch (ProxerException ignored) {

            }

            StorageManager.incrementMessagesInterval();
            NotificationRetrievalManager.retrieveMessagesLater(this);
        } else {
            NotificationRetrievalManager.cancelMessagesRetrieval(this);
        }
    }

}
