package com.proxerme.app.manager;

import android.app.PendingIntent;
import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v7.app.NotificationCompat;

import com.proxerme.app.R;
import com.proxerme.app.activity.DashboardActivity;
import com.proxerme.app.util.MaterialDrawerHelper;
import com.proxerme.app.util.PagingHelper;
import com.proxerme.library.entity.Conference;
import com.proxerme.library.entity.News;
import com.proxerme.library.util.ProxerInfo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import static android.support.v4.app.NotificationCompat.BigTextStyle;

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
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(context);

            builder.setAutoCancel(true)
                    .setContentTitle(context.getString(R.string.news_notification_title))
                    .setSmallIcon(R.drawable.ic_stat_proxer);

            if (offset == 1) {
                News current = news.get(0);

                builder.setContentText(current.getSubject());
                builder.setStyle(new BigTextStyle(builder).bigText(current.getSubject() + "\n\n" +
                        current.getDescription()));
            } else {
                builder.setContentText(generateNewsNotificationAmount(context, offset))
                        .setStyle(new BigTextStyle(builder)
                                .bigText(generateNewsNotificationBigText(news, offset)));
            }

            builder.setContentIntent(PendingIntent.getActivity(
                    context, 0, DashboardActivity.getSectionIntent(context,
                            MaterialDrawerHelper.DRAWER_ID_NEWS, null),
                    PendingIntent.FLAG_UPDATE_CURRENT));

            notificationManager.notify(NEWS_NOTIFICATION, builder.build());
        }
    }

    private static String generateNewsNotificationAmount(@NonNull Context context,
                                                         @IntRange(from = PagingHelper.OFFSET_TOO_LARGE,
                                                                 to = ProxerInfo.CONFERENCES_ON_PAGE) int offset) {
        return offset == PagingHelper.OFFSET_TOO_LARGE ?
                context.getString(R.string.notification_amount_more_than_15) :
                (offset + " " + context.getString(R.string.notification_amount_text));
    }

    private static String generateNewsNotificationBigText(@NonNull List<News> news,
                                                          @IntRange(from = PagingHelper.OFFSET_TOO_LARGE,
                                                                  to = ProxerInfo.CONFERENCES_ON_PAGE) int offset) {
        String result = "";

        for (int i = 0; i < offset; i++) {
            result += news.get(i).getSubject();
            result += '\n';
        }

        return result;
    }

    /**
     * Shows a notification about new messages to the user. If there are none, nothing will happen.
     *
     * @param context     The context.
     * @param conferences The new messages. It is expected, that only new conferences are passes,
     *                    as no checks will hapen here.
     */
    public static void showMessagesNotification(@NonNull Context context,
                                                @NonNull List<Conference> conferences) {
        if (!conferences.isEmpty()) {
            android.app.NotificationManager notificationManager =
                    (android.app.NotificationManager) context
                            .getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(context);

            builder.setAutoCancel(true)
                    .setContentTitle(context.getString(R.string.messages_notification_title))
                    .setSmallIcon(R.drawable.ic_stat_proxer);

            String content = context.getString(R.string.messages_from) + " " +
                    conferences.get(0).getTopic();

            for (int i = 1; i < conferences.size(); i++) {
                content += ", " + conferences.get(i).getTopic();
            }

            builder.setContentText(content);

            builder.setContentIntent(PendingIntent.getActivity(
                    context, 0, DashboardActivity.getSectionIntent(context,
                            MaterialDrawerHelper.DRAWER_ID_MESSAGES, null),
                    PendingIntent.FLAG_UPDATE_CURRENT));

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

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NEWS_NOTIFICATION, MESSAGES_NOTIFICATION})
    public @interface NotificationId {
    }
}
