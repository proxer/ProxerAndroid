package com.rubengees.proxerme.fragment;


import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rubengees.proxerme.R;
import com.rubengees.proxerme.adapter.NewsAdapter;
import com.rubengees.proxerme.connection.ProxerConnection;
import com.rubengees.proxerme.connection.ProxerException;
import com.rubengees.proxerme.entity.News;
import com.rubengees.proxerme.manager.NewsManager;
import com.rubengees.proxerme.util.EndlessRecyclerOnScrollListener;
import com.rubengees.proxerme.util.SnackbarManager;
import com.rubengees.proxerme.util.Utils;

import java.util.List;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class NewsFragment extends Fragment {

    private static final String STATE_NEWS_CURRENT_PAGE = "news_current_page";
    private static final String STATE_NEWS_LAST_LOADED_PAGE = "news_last_loaded_page";

    private RecyclerView list;

    private NewsAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    private boolean loading = false;
    private int currentPage = 1;
    private int lastLoadedPage = -1;

    public NewsFragment() {

    }

    public static NewsFragment newInstance() {
        NewsFragment fragment = new NewsFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            adapter = new NewsAdapter();
        } else {
            adapter = new NewsAdapter(savedInstanceState);
            currentPage = savedInstanceState.getInt(STATE_NEWS_CURRENT_PAGE);
            lastLoadedPage = savedInstanceState.getInt(STATE_NEWS_LAST_LOADED_PAGE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        swipeRefreshLayout = (SwipeRefreshLayout) inflater.inflate(R.layout.fragment_news, container, false);
        list = (RecyclerView) swipeRefreshLayout.findViewById(R.id.fragment_news_list);

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(
                getActivity() == null ? 1 : Utils.calculateSpanAmount(getActivity()),
                StaggeredGridLayoutManager.VERTICAL);

        list.setHasFixedSize(true);
        list.setAdapter(adapter);
        list.setLayoutManager(layoutManager);
        list.addOnScrollListener(new EndlessRecyclerOnScrollListener(layoutManager) {
            @Override
            public void onLoadMore() {
                if (!loading) {
                    loadNews(currentPage, false);
                }
            }
        });

        swipeRefreshLayout.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadNews(1, true);
            }
        });

        if (savedInstanceState == null) {
            loadNews(currentPage, false);
        }

        return swipeRefreshLayout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        adapter.saveInstanceState(outState);
        outState.putInt(STATE_NEWS_CURRENT_PAGE, currentPage);
        outState.putInt(STATE_NEWS_LAST_LOADED_PAGE, lastLoadedPage);
    }

    private void loadNews(@IntRange(from = 1) final int page, final boolean insert) {
        lastLoadedPage = page;
        loading = true;

        swipeRefreshLayout.setRefreshing(true);

        ProxerConnection.loadNews(page, new ProxerConnection.ResultCallback<List<News>>() {
            @Override
            public void onResult(List<News> result) {
                if (!insert) {
                    currentPage++;
                }

                loading = false;
                handleResult(result, insert);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onError(ProxerException exception) {
                SnackbarManager.show(Snackbar.make(swipeRefreshLayout, exception.getMessage(),
                        Snackbar.LENGTH_INDEFINITE), "Retry", new SnackbarManager.SnackbarCallback() {
                    @Override
                    public void onClick(View v) {
                        loadNews(lastLoadedPage, insert);
                    }
                });

                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void handleResult(List<News> result, boolean insert) {
        if (insert) {
            adapter.insertAtStart(result);

            NewsManager.getInstance(getContext()).setLastId(result.get(0).getId());
        } else {
            adapter.append(result);
        }
    }

}
