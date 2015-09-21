package com.rubengees.proxerme.connection;

import android.support.annotation.IntRange;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class UrlHolder {

    public static String getNewsUrl(@IntRange(from = 1) int page){
        return "/notifications?format=json&s=news&p=" + page;
    }

    public static String getNewsImageUrl(int newsId, long imageId){
        return "http://cdn.proxer.me/news/" + newsId + "_" + imageId + ".png";
    }

}
