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
import android.support.annotation.NonNull;

/**
 * Helper class, containing all the different Urls for accessing the API.
 *
 * @author Ruben Gees
 */
public class UrlHolder {

    private static final String HOST = "http://proxer.me";
    private static final String NEWS = "/notifications?format=json&s=news&p=";
    private static final String NEWS_IMAGE = "http://cdn.proxer.me/news/";
    private static final String LOGIN = "/login?format=json&action=login";

    @NonNull
    public static String getHost() {
        return HOST;
    }

    @NonNull
    public static String getNewsUrl(@IntRange(from = 1) int page) {
        return getHost() + NEWS + page;
    }

    @NonNull
    public static String getNewsImageUrl(@NonNull String newsId, @NonNull String imageId) {
        return NEWS_IMAGE + newsId + "_" + imageId + ".png";
    }

    @NonNull
    public static String getNewsPageUrl(@NonNull String categoryId, @NonNull String threadId) {
        return getHost() + "/forum/" + categoryId + "/" + threadId + "#top";
    }

    @NonNull
    public static String getDonateUrl() {
        return getHost() + "/donate";
    }

    @NonNull
    public static String getLoginUrl() {
        return getHost() + LOGIN;
    }

}
