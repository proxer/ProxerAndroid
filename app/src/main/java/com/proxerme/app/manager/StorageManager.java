package com.proxerme.app.manager;

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
public class StorageManager {

    private static final String STORAGE_NEW_NEWS = "storage_news_new";
    private static final String STORAGE_FIRST_START = "storage_first_start";
    private static final String STORAGE_NEWS_LAST_ID = "storage_news_last_id";
    private static final String STORAGE_USER_USERNAME = "storage_user_username";
    private static final String STORAGE_USER_PASSWORD = "storage_user_password";
    private static final String STORAGE_USER_ID = "storage_user_id";

    @Nullable
    public static String getLastId() {
        return Hawk.get(STORAGE_NEWS_LAST_ID, null);
    }

    public static void setLastId(@Nullable String id) {
        Hawk.put(STORAGE_NEWS_LAST_ID, id);
    }

    @IntRange(from = 0)
    public static int getNewNews() {
        return Hawk.get(STORAGE_NEW_NEWS, 0);
    }

    public static void setNewNews(@IntRange(from = 0) int amount) {
        Hawk.put(STORAGE_NEW_NEWS, amount);
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

        if (username == null || password == null) {
            return null;
        } else {
            return new LoginUser(username, password);
        }
    }

    public static void setUser(@NonNull LoginUser user) {
        if (user.getUserId() == null) {
            throw new IllegalArgumentException("The user must have a user id");
        } else {
            Hawk.chain(3).put(STORAGE_USER_USERNAME, user.getUsername())
                    .put(STORAGE_USER_PASSWORD, user.getPassword()).commit();
        }
    }

    public static void removeUser() {
        Hawk.remove(STORAGE_USER_USERNAME, STORAGE_USER_PASSWORD, STORAGE_USER_ID);
    }
}
