package com.proxerme.app.manager;

import android.support.annotation.IntRange;
import android.support.annotation.Nullable;

/**
 * A singleton for managing the news.
 *
 * @author Ruben Gees
 */
public class NewsManager {
    private static NewsManager INSTANCE;

    private String lastId;
    private int newNews = 0;

    private NewsManager() {
        loadId();
        loadNewNews();
    }

    public static NewsManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NewsManager();
        }

        return INSTANCE;
    }

    /**
     * Returns the last retrieved news id.
     *
     * @return The id.
     */
    @Nullable
    public String getLastId() {
        return lastId;
    }

    /**
     * Sets the last retrieved id.
     *
     * @param id The id.
     */
    public void setLastId(@Nullable String id) {
        lastId = id;

        saveId();
    }

    /**
     * Returns the new news since the last query. Those are set with the method
     * {@link #setNewNews(int)}.
     *
     * @return The amount of new News.
     */
    @IntRange(from = 0)
    public int getNewNews() {
        return newNews;
    }

    /**
     * Sets the new news since the last query. To be used in a background service.
     *
     * @param newNews The amount of new News.
     */
    public void setNewNews(@IntRange(from = 0) int newNews) {
        this.newNews = newNews;

        saveNewNews();
    }

    private void saveId() {
        StorageManager.setLastNewsId(lastId);
    }

    private void loadId() {
        lastId = StorageManager.getLastNewsId();
    }

    private void saveNewNews() {
        StorageManager.setNewNews(newNews);
    }

    private void loadNewNews() {
        newNews = StorageManager.getNewNews();
    }
}
