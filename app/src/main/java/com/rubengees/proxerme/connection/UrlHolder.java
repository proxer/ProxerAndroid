package com.rubengees.proxerme.connection;

import android.support.annotation.IntRange;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class UrlHolder {

    public static String getHost() {
        return "http://proxer.me";
    }

    public static String getNewsUrl(@IntRange(from = 1) int page){
        return getHost() + "/notifications?format=json&s=news&p=" + page;
    }

    public static String getNewsImageUrl(int newsId, String imageId) {
        return "http://cdn.proxer.me/news/" + newsId + "_" + imageId + ".png";
    }

    public static String getNewsPageUrl(int categoryId, int threadId) {
        return getHost() + "/forum/" + categoryId + "/" + threadId;
    }

}
