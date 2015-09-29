/*
 *   Copyright 2015 Ruben Gees
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

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

    public static String getDonateUrl() {
        return getHost() + "/donate";
    }

}
