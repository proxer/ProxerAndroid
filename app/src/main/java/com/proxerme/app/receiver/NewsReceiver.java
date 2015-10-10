package com.proxerme.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.proxerme.app.service.NewsService;

/**
 * Receiver for the {@link NewsService}.
 *
 * @author Ruben Gees
 */
public class NewsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NewsService.startActionLoadNews(context);
    }
}
