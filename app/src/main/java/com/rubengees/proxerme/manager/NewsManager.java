package com.rubengees.proxerme.manager;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.rubengees.proxerme.entity.News;

import java.util.List;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class NewsManager {
    private static NewsManager INSTANCE;

    public static final int OFFSET_NOT_CALCULABLE = -2;
    public static final int OFFSET_TOO_LARGE = -1;
    public static final int NEWS_ON_PAGE = 15;

    private int lastId;

    public static NewsManager getInstance(@NonNull Activity context) {
        if (INSTANCE == null) {
            INSTANCE = new NewsManager(context);
        }
        return INSTANCE;
    }

    private Activity context;

    private NewsManager(@NonNull Activity context) {
        this.context = context;

        loadId();
    }

    public static int calculateOffsetFromStart(@NonNull List<News> list, @NonNull News last) {
        if (list.isEmpty()) {
            return OFFSET_NOT_CALCULABLE;
        } else {
            for (int i = 0; i < list.size() && i < NEWS_ON_PAGE; i++) {
                if (last.getId() == list.get(i).getId()) {
                    return i + 1;
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

    public void setLastId(int id){
        lastId = id;

        saveId();
    }

    public int getLastId(){
        return lastId;
    }

    private void saveId(){
        SharedPreferences preferences = context.getPreferences(Context.MODE_PRIVATE);

        preferences.edit().putInt("news_last_id", lastId).apply();
    }

    private void loadId(){
        SharedPreferences preferences = context.getPreferences(Context.MODE_PRIVATE);

        preferences.getInt("news_last_id", -1);
    }
}
