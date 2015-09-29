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

package com.rubengees.proxerme.fragment;

import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.rubengees.proxerme.R;
import com.rubengees.proxerme.activity.DashboardActivity;
import com.rubengees.proxerme.activity.NewsImageDetailActivity;
import com.rubengees.proxerme.adapter.NewsAdapter;
import com.rubengees.proxerme.connection.ErrorHandler;
import com.rubengees.proxerme.connection.ProxerConnection;
import com.rubengees.proxerme.connection.ProxerException;
import com.rubengees.proxerme.connection.UrlHolder;
import com.rubengees.proxerme.entity.News;
import com.rubengees.proxerme.manager.NewsManager;
import com.rubengees.proxerme.util.EndlessRecyclerOnScrollListener;
import com.rubengees.proxerme.util.SnackbarManager;
import com.rubengees.proxerme.util.Utils;

import java.util.List;

import static com.rubengees.proxerme.manager.NewsManager.getInstance;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class NewsFragment extends MainFragment {

    public static final String STATE_METHOD_BEFORE_ERROR = "news_method_before_error";
    private static final String STATE_CURRENT_PAGE = "news_current_page";
    private static final String STATE_LAST_LOADED_PAGE = "news_last_loaded_page";
    private static final String STATE_ERROR_MESSAGE = "news_error_message";
    private NewsAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    private boolean loading = false;
    private int currentPage = 1;
    private int lastLoadedPage = -1;

    private String currentErrorMessage;
    private boolean methodBeforeErrorInsert = false;
    private View root;

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
            currentPage = savedInstanceState.getInt(STATE_CURRENT_PAGE);
            lastLoadedPage = savedInstanceState.getInt(STATE_LAST_LOADED_PAGE);
            currentErrorMessage = savedInstanceState.getString(STATE_ERROR_MESSAGE);
            methodBeforeErrorInsert = savedInstanceState.getBoolean(STATE_METHOD_BEFORE_ERROR);
        }

        adapter.setOnNewsInteractionListener(new NewsAdapter.OnNewsInteractionListener() {
            @Override
            public void onNewsClick(@NonNull View v, @NonNull News news) {
                getDashboardActivity().showPage(UrlHolder.getNewsPageUrl(news.getCategoryId(),
                        news.getThreadId()));
            }

            @Override
            public void onNewsImageClick(@NonNull View v, @NonNull News news) {
                NewsImageDetailActivity.navigateTo(getActivity(), (ImageView) v, news);
            }

            @Override
            public void onNewsExpanded(@NonNull View v, @NonNull News news) {
                getDashboardActivity().setLikelyUrl(UrlHolder.getNewsPageUrl(news.getCategoryId(),
                        news.getThreadId()));
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.fragment_news, container, false);
        swipeRefreshLayout = (SwipeRefreshLayout) root
                .findViewById(R.id.fragment_news_list_container);
        RecyclerView list = (RecyclerView) swipeRefreshLayout.findViewById(R.id.fragment_news_list);

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
        } else if (currentErrorMessage != null) {
            showError();
        } else {
            loadNews(currentPage, methodBeforeErrorInsert);
        }

        return root;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        adapter.saveInstanceState(outState);
        outState.putInt(STATE_CURRENT_PAGE, currentPage);
        outState.putInt(STATE_LAST_LOADED_PAGE, lastLoadedPage);
        outState.putString(STATE_ERROR_MESSAGE, currentErrorMessage);
        outState.putBoolean(STATE_METHOD_BEFORE_ERROR, methodBeforeErrorInsert);
    }

    private void loadNews(@IntRange(from = 1) final int page, final boolean insert) {
        lastLoadedPage = page;
        loading = true;

        swipeRefreshLayout.setRefreshing(true);

        ProxerConnection.loadNews(page, new ProxerConnection.ResultCallback<List<News>>() {
            @Override
            public void onResult(@NonNull List<News> result) {
                if (!insert) {
                    currentPage++;
                }

                loading = false;
                currentErrorMessage = null;
                getDashboardActivity().setBadge(DashboardActivity.DRAWER_ID_NEWS, null);
                NewsManager manager = getInstance(getContext());

                manager.setNewNews(0);
                manager.setLastId(result.get(0).getId());
                manager.retrieveNewsLater();

                handleResult(result, insert);

                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onError(@NonNull ProxerException exception) {
                if (exception.getErrorCode() == ErrorHandler.ErrorCodes.PROXER) {
                    currentErrorMessage = exception.getMessage();
                } else {
                    currentErrorMessage = ErrorHandler.getMessageForErrorCode(getContext(),
                            exception.getErrorCode());
                }

                methodBeforeErrorInsert = insert;

                showError();
            }
        });
    }

    @Override
    public void showErrorIfNecessary() {
        if (currentErrorMessage != null) {
            showError();
        }
    }

    private void handleResult(List<News> result, boolean insert) {
        if (insert) {
            adapter.insertAtStart(result);

            getInstance(getContext()).setLastId(result.get(0).getId());
        } else {
            adapter.append(result);
        }
    }

    private void showError() {
        SnackbarManager.show(Snackbar.make(root, currentErrorMessage,
                        Snackbar.LENGTH_INDEFINITE),
                getContext().getString(R.string.error_retry),
                new SnackbarManager.SnackbarCallback() {
                    @Override
                    public void onClick(View v) {
                        loadNews(lastLoadedPage, methodBeforeErrorInsert);
                    }
                });

        swipeRefreshLayout.setRefreshing(false);
    }
}
