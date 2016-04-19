package com.proxerme.app.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;

import com.proxerme.app.application.MainApplication;
import com.proxerme.app.util.Section;
import com.proxerme.app.util.helper.NotificationHelper;
import com.proxerme.app.util.helper.PagingHelper;
import com.proxerme.app.util.helper.StorageHelper;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerException;
import com.proxerme.library.entity.Conference;
import com.proxerme.library.entity.LoginUser;
import com.proxerme.library.entity.News;
import com.proxerme.library.event.success.LoginEvent;
import com.proxerme.library.util.ProxerInfo;

import org.greenrobot.eventbus.EventBus;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * An IntentService, which retrieves the News and shows a notification if there are unread
 * ones.
 *
 * @author Ruben Gees
 */
public class NotificationService extends IntentService {

    public static final String ACTION_LOAD_NEWS =
            "com.proxerme.app.service.action.LOAD_NEWS";
    public static final String ACTION_LOAD_CONFERENCES =
            "com.proxerme.app.service.action.LOAD_CONFERENCES";
    private static final String SERVICE_TITLE = "Notification Service";

    public NotificationService() {
        super(SERVICE_TITLE);
    }

    public static void load(@NonNull Context context, @NonNull @NotificationAction String action) {
        Intent intent = new Intent(context, NotificationService.class);

        intent.setAction(action);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

            if (ACTION_LOAD_NEWS.equals(action)) {
                if (getMainApplication().getCurrentSection() != Section.NEWS) {
                    handleActionLoadNews();
                }
            } else if (ACTION_LOAD_CONFERENCES.equals(action)) {
                if (getMainApplication().getCurrentSection() != Section.CONFERENCES &&
                        getMainApplication().getCurrentSection() != Section.MESSAGES) {
                    handleActionLoadConferences();
                }
            }
        }
    }

    private void handleActionLoadNews() {
        String lastId = StorageHelper.getLastNewsId();

        try {
            if (lastId != null) {
                List<News> news = ProxerConnection.loadNews(1).executeSynchronized();
                news = news.subList(0, PagingHelper.calculateOffsetFromStart(news, lastId,
                        ProxerInfo.NEWS_ON_PAGE));

                int previousNewNews = StorageHelper.getNewNews();

                if (news.size() > previousNewNews) {
                    StorageHelper.setNewNews(news.size());
                    NotificationHelper.showNewsNotification(this, news);
                }
            }
        } catch (ProxerException ignored) {

        }
    }

    private void handleActionLoadConferences() {
        LoginUser user = StorageHelper.getUser();

        if (user != null) {
            try {
                user = ProxerConnection.login(user).executeSynchronized();
                List<Conference> conferences = ProxerConnection.loadConferences(1)
                        .executeSynchronized();

                EventBus.getDefault().post(new LoginEvent(user));

                for (int i = 0; i < conferences.size(); i++) {
                    if (conferences.get(i).isRead()) {
                        conferences = conferences.subList(0, i);

                        break;
                    }
                }

                if (conferences.size() > 0) {
                    long lastReceivedMessageTime = StorageHelper.getLastReceivedMessageTime();

                    if (lastReceivedMessageTime != -1L &&
                            lastReceivedMessageTime != conferences.get(0).getTime()) {
                        StorageHelper.setLastReceivedMessageTime(conferences.get(0).getTime());

                        NotificationHelper.showMessagesNotification(this, conferences);
                        StorageHelper.setNewMessages(conferences.size());
                    }
                }
            } catch (ProxerException ignored) {

            }

            StorageHelper.incrementMessagesInterval();
            getMainApplication().getNotificationManager().retrieveConferencesLater(this);
        } else {
            getMainApplication().getNotificationManager().cancelMessagesRetrieval(this);
        }
    }

    protected final MainApplication getMainApplication() {
        return (MainApplication) getApplication();
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({ACTION_LOAD_NEWS, ACTION_LOAD_CONFERENCES})
    public @interface NotificationAction {
    }

}
