package com.rubengees.proxerme.connection;

import android.support.annotation.IntRange;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class UrlHolder {

    public static final String HOST = "http://proxer.me";
    public static final String NEWS = "/notifications?format=json&s=news&p=";
    public static final String NEWS_IMAGE = "http://cdn.proxer.me/news/";

    public static String getHost() {
        return HOST;
    }

    public static String getNewsUrl(@IntRange(from = 1) int page){
        return getHost() + NEWS + page;
    }

    public static String getNewsImageUrl(int newsId, String imageId) {
        return NEWS_IMAGE + newsId + "_" + imageId + ".png";
    }

    public static String getNewsPageUrl(int categoryId, int threadId) {
        return getHost() + "/forum/" + categoryId + "/" + threadId + "#top";
    }

}
