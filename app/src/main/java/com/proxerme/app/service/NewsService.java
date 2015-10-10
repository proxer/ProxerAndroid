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

package com.proxerme.app.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.afollestad.bridge.BridgeException;
import com.proxerme.app.manager.NewsManager;
import com.proxerme.app.manager.NotificationManager;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.entity.News;

import org.json.JSONException;

import java.util.List;

/**
 * An {@link IntentService}, which retrieves the News and shows a notification,if there are unread
 * ones.
 *
 * @author Ruben Gees
 */
public class NewsService extends IntentService {

    private static final String ACTION_LOAD_NEWS = "com.proxerme.app.service.action.LOAD_NEWS";

    public NewsService() {
        super("NewsService");
    }

    public static void startActionLoadNews(Context context) {
        Intent intent = new Intent(context, NewsService.class);
        intent.setAction(ACTION_LOAD_NEWS);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

            if (ACTION_LOAD_NEWS.equals(action)) {
                handleActionLoadNews();
            }
        }
    }

    private void handleActionLoadNews() {
        ProxerConnection.init();
        NewsManager manager = NewsManager.getInstance(this);

        try {
            String lastId = manager.getLastId();

            if (lastId != null) {
                List<News> news = ProxerConnection.loadNewsSync(1);
                int offset = NewsManager.calculateOffsetFromStart(news, manager.getLastId());

                manager.setLastId(news.get(0).getId());
                manager.setNewNews(offset);
                NotificationManager.showNewsNotification(this, news, offset);
            }
        } catch (BridgeException | JSONException e) {
            //ignore
        }
    }

}
