package com.proxerme.app.fragment;

import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;

import com.proxerme.app.activity.ImageDetailActivity;
import com.proxerme.app.adapter.NewsAdapter;
import com.proxerme.app.manager.NewsManager;
import com.proxerme.app.manager.NotificationManager;
import com.proxerme.app.manager.NotificationRetrievalManager;
import com.proxerme.app.util.Utils;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerTag;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.entity.News;
import com.proxerme.library.event.error.NewsErrorEvent;
import com.proxerme.library.event.success.NewsEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * A Fragment, retrieving and displaying News.
 *
 * @author Ruben Gees
 */
public class NewsFragment extends PagingFragment<News, NewsAdapter, NewsEvent, NewsErrorEvent> {

    @NonNull
    public static NewsFragment newInstance() {
        return new NewsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NotificationManager.cancel(getContext(), NotificationManager.NEWS_NOTIFICATION);
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

    @Override
    protected void cancelRequest() {
        ProxerConnection.cancel(ProxerTag.NEWS);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onLoad(NewsEvent result) {
        handleResult(result);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onLoadError(NewsErrorEvent errorResult) {
        handleError(errorResult);
    }

    @Override
    protected void handleResult(NewsEvent result) {
        super.handleResult(result);

        NewsManager manager = NewsManager.getInstance();

        manager.setNewNews(0);
        manager.setLastId(result.getItem().get(0).getId());

        NotificationRetrievalManager.retrieveNewsLater(getContext());
    }
}
