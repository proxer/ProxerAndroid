package com.rubengees.proxerme.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.rubengees.proxerme.entity.News;

import java.util.List;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class NewsManager {
    public static final int OFFSET_NOT_CALCULABLE = -2;
    public static final int OFFSET_TOO_LARGE = -1;
    public static final int NEWS_ON_PAGE = 15;
    private static final String PREFERENCE_NEWS_LAST_ID = "news_last_id";
    private static NewsManager INSTANCE;
    private int lastId;
    private Context context;

    private NewsManager(@NonNull Context context) {
        this.context = context;

        loadId();
    }

    public static NewsManager getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            INSTANCE = new NewsManager(context);
        }
        return INSTANCE;
    }

    public static int calculateOffsetFromStart(@NonNull List<News> list, @NonNull News last) {
        if (list.isEmpty()) {
            return OFFSET_NOT_CALCULABLE;
        } else {
            for (int i = 0; i < list.size() || i < NEWS_ON_PAGE; i++) {
                if (last.getId() == list.get(i).getId()) {
                    return NEWS_ON_PAGE - i - 1;
                }
            }

            return OFFSET_TOO_LARGE;
        }

    }

    public static int calculateOffsetFromEnd(@NonNull List<News> list, @NonNull News first) {
        if (list.isEmpty()) {
            return OFFSET_NOT_CALCULABLE;
        } else {
            int lastSearchableIndex = list.size() - NEWS_ON_PAGE;

            for (int i = list.size() - 1; i >= 0 && i >= lastSearchableIndex; i--) {
                if (first.getId() == list.get(i).getId()) {
                    return NEWS_ON_PAGE - i;
                }
            }

            return OFFSET_TOO_LARGE;
        }
    }

    public int getLastId() {
        return lastId;
    }

    public void setLastId(int id){
        lastId = id;

        saveId();
    }

    private void saveId(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        preferences.edit().putInt(PREFERENCE_NEWS_LAST_ID, lastId).apply();
    }

    private void loadId(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        preferences.getInt(PREFERENCE_NEWS_LAST_ID, -1);
    }
}
