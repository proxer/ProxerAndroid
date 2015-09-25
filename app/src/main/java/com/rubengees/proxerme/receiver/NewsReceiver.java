package com.rubengees.proxerme.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.rubengees.proxerme.service.NewsService;

public class NewsReceiver extends BroadcastReceiver {
    public NewsReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        NewsService.startActionLoadNews(context);
    }
}
