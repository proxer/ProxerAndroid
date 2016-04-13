package com.proxerme.app.manager;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v7.app.NotificationCompat;

import com.proxerme.app.R;
import com.proxerme.app.activity.DashboardActivity;
import com.proxerme.app.activity.MessageActivity;
import com.proxerme.app.util.helper.MaterialDrawerHelper;
import com.proxerme.app.util.helper.PagingHelper;
import com.proxerme.library.entity.Conference;
import com.proxerme.library.entity.News;
import com.proxerme.library.util.ProxerInfo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import static android.support.v4.app.NotificationCompat.BigTextStyle;
import static android.support.v4.app.NotificationCompat.InboxStyle;
import static android.support.v4.app.NotificationCompat.Style;


/**
 * A helper class for displaying notifications.
 *
 * @author Ruben Gees
 */
public class NotificationManager {

    public static final int NEWS_NOTIFICATION = 1423;
    public static final int MESSAGES_NOTIFICATION = 1424;

    /**
     * Shows a Notification about news to the user. If there are no new news, nothing will be shown.
     *
     * @param context The Context.
     * @param news    The List of {@link News}.
     * @param offset  The offset to the last retrieved news.
     */
    public static void showNewsNotification(@NonNull Context context, @NonNull List<News> news,
                                            @IntRange(from = PagingHelper.OFFSET_NOT_CALCULABLE,
                                                    to = ProxerInfo.CONFERENCES_ON_PAGE) int offset) {
        if (offset != PagingHelper.OFFSET_NOT_CALCULABLE && offset > 0) {
            android.app.NotificationManager notificationManager =
                    (android.app.NotificationManager) context
                            .getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            Style style;

            if (offset == 1) {
                News current = news.get(0);
                String title = current.getSubject().trim();
                String content = current.getDescription().trim();

                builder.setContentTitle(title);
                builder.setContentText(content);

                style = new BigTextStyle(builder).bigText(content)
                        .setBigContentTitle(title)
                        .setSummaryText(generateNewsNotificationAmount(context, offset));
            } else {
                InboxStyle inboxStyle = new InboxStyle();

                for (int i = 0; i < 5 && i < offset; i++) {
                    inboxStyle.addLine(news.get(i).getSubject());
                }

                inboxStyle.setBigContentTitle(context
                        .getString(R.string.news_notification_title))
                        .setSummaryText(generateNewsNotificationAmount(context, offset));

                style = inboxStyle;
            }

            builder.setAutoCancel(true).setSmallIcon(R.drawable.ic_stat_proxer)
                    .setContentTitle(context.getString(R.string.news_notification_title))
                    .setContentText(generateNewsNotificationAmount(context, offset))
                    .setContentIntent(PendingIntent.getActivity(context, 0,
                            DashboardActivity.getSectionIntent(context,
                                    MaterialDrawerHelper.DRAWER_ID_NEWS, null),
                            PendingIntent.FLAG_UPDATE_CURRENT))
                    .setStyle(style);

            notificationManager.notify(NEWS_NOTIFICATION, builder.build());
        }
    }

    /**
     * Shows a notification about new messages to the user. If there are none, nothing will happen.
     *
     * @param context     The context.
     * @param conferences The new messages. It is expected that only new conferences are passed
     *                    as no checks will happen here.
     */
    public static void showMessagesNotification(@NonNull Context context,
                                                @NonNull List<Conference> conferences) {
        if (!conferences.isEmpty()) {
            android.app.NotificationManager notificationManager =
                    (android.app.NotificationManager) context
                            .getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            InboxStyle inboxStyle = new InboxStyle();
            String amount = context.getResources()
                    .getQuantityString(R.plurals.messages_notification_amount_text,
                            conferences.size(), conferences.size());
            PendingIntent intent;

            inboxStyle.setBigContentTitle(context.getString(R.string.messages_notification_title))
                    .setSummaryText(amount);

            for (int i = 0; i < 5 && i < conferences.size(); i++) {
                inboxStyle.addLine(conferences.get(i).getTopic());
            }

            if (conferences.size() == 1) {
                intent = PendingIntent.getActivity(context, 0, MessageActivity.getIntent(context,
                        conferences.get(0)), PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                intent = PendingIntent.getActivity(
                        context, 0, DashboardActivity.getSectionIntent(context,
                                MaterialDrawerHelper.DRAWER_ID_MESSAGES, null),
                        PendingIntent.FLAG_UPDATE_CURRENT);
            }

            builder.setContentTitle(context.getString(R.string.messages_notification_title))
                    .setContentText(amount)
                    .setSmallIcon(R.drawable.ic_stat_proxer)
                    .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND |
                            Notification.DEFAULT_LIGHTS)
                    .setContentIntent(intent)
                    .setStyle(inboxStyle)
                    .setAutoCancel(true);

            notificationManager.notify(MESSAGES_NOTIFICATION, builder.build());
        }
    }

    /**
     * Cancels an existing notification. If the notification is not currently displayed, nothing
     * will happen.
     *
     * @param context The context.
     * @param id      The id of the notification.
     */
    public static void cancel(@NonNull Context context, @NotificationId int id) {
        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) context
                        .getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancel(id);
    }

    private static String generateNewsNotificationAmount(@NonNull Context context,
                                                         @IntRange(from = PagingHelper.OFFSET_TOO_LARGE,
                                                                 to = ProxerInfo.CONFERENCES_ON_PAGE)
                                                         int offset) {
        return offset == PagingHelper.OFFSET_TOO_LARGE ?
                context.getString(R.string.news_notification_amount_more_than_15) :
                (context.getResources().getQuantityString(R.plurals.news_notification_amount_text,
                        offset, offset));
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NEWS_NOTIFICATION, MESSAGES_NOTIFICATION})
    public @interface NotificationId {
    }
}
