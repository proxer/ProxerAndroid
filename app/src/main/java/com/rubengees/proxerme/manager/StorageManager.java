package com.rubengees.proxerme.manager;

import android.support.annotation.IntRange;
import android.support.annotation.Nullable;

import com.orhanobut.hawk.Hawk;

/**
 * Todo: Describe Class
 *
 * @author Ruben Gees
 */
public class StorageManager {

    public static final String STORAGE_NEW_NEWS = "storage_news_new";
    public static final String STORAGE_FIRST_START = "storage_first_start";
    public static final String STORAGE_NEWS_LAST_ID = "storage_news_last_id";

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
}
