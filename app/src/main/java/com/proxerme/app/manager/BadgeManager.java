package com.proxerme.app.manager;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.proxerme.app.util.helper.MaterialDrawerHelper;
import com.proxerme.app.util.helper.PagingHelper;
import com.proxerme.library.event.success.ConferencesEvent;
import com.proxerme.library.event.success.NewsEvent;
import com.proxerme.library.util.ProxerInfo;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.proxerme.app.util.helper.MaterialDrawerHelper.DrawerItemId;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public class BadgeManager {

    private BadgeCallback callback;

    public BadgeManager(@NonNull BadgeCallback callback) {
        this.callback = callback;

        init();
    }

    public void startListenForEvents() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    public void stopListenForEvents() {
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewsLoaded(NewsEvent event) {
        callback.updateBadge(MaterialDrawerHelper.DRAWER_ID_NEWS, null);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConferencesLoaded(ConferencesEvent event) {
        callback.updateBadge(MaterialDrawerHelper.DRAWER_ID_MESSAGES, null);
    }

    private void init() {
        int newNews = NewsManager.getInstance().getNewNews();
        int newMessages = StorageManager.getNewMessages();

        if (newNews > 0 || newNews == PagingHelper.OFFSET_NOT_CALCULABLE) {
            callback.updateBadge(MaterialDrawerHelper.DRAWER_ID_NEWS,
                    newNews == PagingHelper.OFFSET_NOT_CALCULABLE ?
                            (ProxerInfo.NEWS_ON_PAGE + "+") : (String.valueOf(newNews)));
        }

        if (newMessages > 0) {
            callback.updateBadge(MaterialDrawerHelper.DRAWER_ID_MESSAGES,
                    String.valueOf(newMessages));
        }
    }

    public interface BadgeCallback {
        void updateBadge(@DrawerItemId int id, @Nullable String count);
    }

}
