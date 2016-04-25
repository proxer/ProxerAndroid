package com.proxerme.app.fragment;

import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;

import com.proxerme.app.R;
import com.proxerme.app.activity.ImageDetailActivity;
import com.proxerme.app.adapter.NewsAdapter;
import com.proxerme.app.util.EventBusBuffer;
import com.proxerme.app.util.Section;
import com.proxerme.app.util.Utils;
import com.proxerme.app.util.helper.StorageHelper;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerTag;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.entity.News;
import com.proxerme.library.event.error.NewsErrorEvent;
import com.proxerme.library.event.success.NewsEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

/**
 * A Fragment, retrieving and displaying News.
 *
 * @author Ruben Gees
 */
public class NewsFragment extends PagingFragment<News, NewsAdapter, NewsEvent, NewsErrorEvent> {

    private EventBusBuffer eventBusBuffer = new EventBusBuffer() {
        @Subscribe
        public void onLoad(NewsEvent event) {
            addToQueue(event);
        }

        @Subscribe
        public void onLoadError(NewsErrorEvent event) {
            addToQueue(event);
        }
    };

    @NonNull
    public static NewsFragment newInstance() {
        return new NewsFragment();
    }

    @Override
    public void onResume() {
        super.onResume();

        getMainApplication().setCurrentSection(Section.NEWS);
    }

    @Override
    protected void configAdapter(@NonNull NewsAdapter adapter) {
        adapter.setOnNewsInteractionListener(new NewsAdapter.OnNewsInteractionListener() {
            @Override
            public void onNewsClick(@NonNull View v, @NonNull News news) {
                if (Utils.areActionsPossible(getActivity())) {
                    getParentActivity().showPage(UrlHolder.getSingleNewsUrlWeb(news.getCategoryId(),
                            news.getThreadId()));
                }
            }

            @Override
            public void onNewsImageClick(@NonNull View v, @NonNull News news) {
                if (Utils.areActionsPossible(getActivity())) {
                    ImageDetailActivity.navigateTo(getActivity(), (ImageView) v,
                            UrlHolder.getNewsImageUrl(news.getId(), news.getImageId()));
                }
            }

            @Override
            public void onNewsExpanded(@NonNull View v, @NonNull News news) {
                getParentActivity().setLikelyUrl(UrlHolder
                        .getSingleNewsUrlWeb(news.getCategoryId(), news.getThreadId()));
            }
        });
    }

    @Override
    protected NewsAdapter createAdapter(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return new NewsAdapter();
        } else {
            return new NewsAdapter(savedInstanceState);
        }
    }

    @Override
    protected void load(@IntRange(from = 1) int page, boolean insert) {
        ProxerConnection.loadNews(page).execute();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoad(NewsEvent result) {
        handleResult(result);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoadError(NewsErrorEvent errorResult) {
        handleError(errorResult);
    }

    @Override
    protected void cancelRequest() {
        ProxerConnection.cancel(ProxerTag.NEWS);
    }

    @Override
    protected void handleResult(List<News> result, boolean insert) {
        super.handleResult(result, insert);

        if (insert) {
            StorageHelper.setNewNews(0);

            if (result.size() > 0) {
                StorageHelper.setLastNewsId(result.get(0).getId());
            }

            getMainApplication().getNotificationManager().retrieveNewsLater(getContext());
        }
    }

    @NonNull
    @Override
    protected String getNotificationText(int amount) {
        return getResources().getQuantityString(R.plurals.notification_news, amount, amount);
    }

    @Override
    protected EventBusBuffer getEventBusBuffer() {
        return eventBusBuffer;
    }
}
