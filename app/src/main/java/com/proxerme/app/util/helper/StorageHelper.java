package com.proxerme.app.util.helper;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.orhanobut.hawk.Hawk;
import com.proxerme.library.entity.LoginUser;

/**
 * A helper class, giving access to the storage.
 *
 * @author Ruben Gees
 */
public class StorageHelper {

    public static final String STORAGE_MESSAGES_NOTIFICATIONS_INTERVAL =
            "storage_messages_notifications_interval";
    public static final String STORAGE_USER_IMAGE_ID = "storage_user_image_id";
    private static final String STORAGE_NEW_NEWS = "storage_news_new";
    private static final String STORAGE_FIRST_START = "storage_first_start";
    private static final String STORAGE_NEWS_LAST_ID = "storage_news_last_id";
    private static final String STORAGE_USER_USERNAME = "storage_user_username";
    private static final String STORAGE_USER_PASSWORD = "storage_user_password";
    private static final String STORAGE_USER_ID = "storage_user_id";
    private static final String STORAGE_NEW_MESSAGES = "storage_messages_new";
    private static final String STORAGE_LAST_LOGIN = "storage_last_login";

    private static final int MAX_MESSAGE_POLLING_INTERVAL = 850;
    private static final int DEFAULT_MESSAGES_INTERVAL = 5;
    private static final double MESSAGES_INTERVAL_MULTIPLICAND = 1.5;

    @Nullable
    public static String getLastNewsId() {
        return Hawk.get(STORAGE_NEWS_LAST_ID, null);
    }

    public static void setLastNewsId(@Nullable String id) {
        if (id == null) {
            Hawk.remove(STORAGE_NEWS_LAST_ID);
        } else {
            Hawk.put(STORAGE_NEWS_LAST_ID, id);
        }
    }

    @IntRange(from = 0)
    public static int getNewNews() {
        return Hawk.get(STORAGE_NEW_NEWS, 0);
    }

    public static void setNewNews(@IntRange(from = 0) int amount) {
        Hawk.put(STORAGE_NEW_NEWS, amount);
    }

    @IntRange(from = 0)
    public static int getNewMessages() {
        return Hawk.get(STORAGE_NEW_MESSAGES, 0);
    }

    public static void setNewMessages(@IntRange(from = 0) int amount) {
        Hawk.put(STORAGE_NEW_MESSAGES, amount);
    }

    public static void setFirstStartOccurred() {
        Hawk.put(STORAGE_FIRST_START, false);
    }

    public static boolean isFirstStart() {
        return Hawk.get(STORAGE_FIRST_START, true);
    }

    @Nullable
    public static LoginUser getUser() {
        String username = Hawk.get(STORAGE_USER_USERNAME);
        String password = Hawk.get(STORAGE_USER_PASSWORD);
        String id = Hawk.get(STORAGE_USER_ID);
        String imageId = Hawk.get(STORAGE_USER_IMAGE_ID);

        if (username == null || password == null || id == null || imageId == null) {
            return null;
        } else {
            return new LoginUser(username, password, id, imageId);
        }
    }

    public static void setUser(@NonNull LoginUser user) {
        Hawk.chain(4).put(STORAGE_USER_USERNAME, user.getUsername())
                .put(STORAGE_USER_PASSWORD, user.getPassword()).put(STORAGE_USER_ID, user.getId())
                .put(STORAGE_USER_IMAGE_ID, user.getImageId()).commit();
    }

    public static void removeUser() {
        Hawk.remove(STORAGE_USER_USERNAME, STORAGE_USER_PASSWORD, STORAGE_USER_ID,
                STORAGE_USER_IMAGE_ID);
    }

    public static void incrementMessagesInterval() {
        int interval = getMessagesInterval();

        if (interval <= MAX_MESSAGE_POLLING_INTERVAL) {
            Hawk.put(STORAGE_MESSAGES_NOTIFICATIONS_INTERVAL,
                    (int) (interval * MESSAGES_INTERVAL_MULTIPLICAND));
        }
    }

    public static void resetMessagesInterval() {
        Hawk.put(STORAGE_MESSAGES_NOTIFICATIONS_INTERVAL, DEFAULT_MESSAGES_INTERVAL);
    }

    @IntRange(from = 5)
    public static int getMessagesInterval() {
        return Hawk.get(STORAGE_MESSAGES_NOTIFICATIONS_INTERVAL, DEFAULT_MESSAGES_INTERVAL);
    }

    public static long getLastLogin() {
        return Hawk.get(STORAGE_LAST_LOGIN, -1L);
    }

    public static void setLastLogin(long lastLogin) {
        Hawk.put(STORAGE_LAST_LOGIN, lastLogin);
    }
}
