package com.proxerme.app.fragment;

import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.proxerme.app.R;
import com.proxerme.app.activity.NewsImageDetailActivity;
import com.proxerme.app.adapter.NewsAdapter;
import com.proxerme.app.manager.NewsManager;
import com.proxerme.app.util.EndlessRecyclerOnScrollListener;
import com.proxerme.app.util.MaterialDrawerHelper;
import com.proxerme.app.util.SnackbarManager;
import com.proxerme.app.util.Utils;
import com.proxerme.library.connection.ErrorHandler;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerException;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.entity.News;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.proxerme.app.manager.NewsManager.getInstance;
import static com.proxerme.app.util.ErrorHandler.getMessageForErrorCode;

/**
 * A {@link Fragment], retrieving and displaying News.
 *
 * @author Ruben Gees
 */
public class NewsFragment extends DashboardFragment {

    public static final String STATE_NEWS_LOADING = "news_loading";
    private static final String STATE_METHOD_BEFORE_ERROR = "news_method_before_error";
    private static final String STATE_CURRENT_PAGE = "news_current_page";
    private static final String STATE_LAST_LOADED_PAGE = "news_last_loaded_page";
    private static final String STATE_ERROR_MESSAGE = "news_error_message";
    View root;

    @Bind(R.id.fragment_news_list_container)
    SwipeRefreshLayout swipeRefreshLayout;

    @Bind(R.id.fragment_news_list)
    RecyclerView list;

    private NewsAdapter adapter;

    private boolean loading = false;
    private int currentPage = 1;
    private int lastLoadedPage = 1;
    private String currentErrorMessage;
    private boolean methodBeforeErrorInsert = false;

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
            loading = savedInstanceState.getBoolean(STATE_NEWS_LOADING);
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

        ButterKnife.bind(this, root);

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
        } else if (loading) {
            loadNews(currentPage, methodBeforeErrorInsert);
        }

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        root = null;
        ButterKnife.unbind(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        adapter.saveInstanceState(outState);
        outState.putBoolean(STATE_NEWS_LOADING, loading);
        outState.putInt(STATE_CURRENT_PAGE, currentPage);
        outState.putInt(STATE_LAST_LOADED_PAGE, lastLoadedPage);
        outState.putString(STATE_ERROR_MESSAGE, currentErrorMessage);
        outState.putBoolean(STATE_METHOD_BEFORE_ERROR, methodBeforeErrorInsert);
    }

    private void loadNews(@IntRange(from = 1) final int page, final boolean insert) {
        lastLoadedPage = page;
        loading = true;
        currentErrorMessage = null;

        swipeRefreshLayout.setRefreshing(true);

        ProxerConnection.loadNews(page).execute(new ProxerConnection.ResultCallback<List<News>>() {
            @Override
            public void onResult(List<News> result) {
                if (!insert) {
                    currentPage++;
                }

                loading = false;
                getDashboardActivity().setBadge(MaterialDrawerHelper.DRAWER_ID_NEWS, null);
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
                    currentErrorMessage = getMessageForErrorCode(getContext(),
                            exception.getErrorCode());
                }

                loading = false;
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
        if (!SnackbarManager.isShowing()) {
            SnackbarManager.show(Snackbar.make(root, currentErrorMessage,
                            Snackbar.LENGTH_INDEFINITE),
                    getContext().getString(R.string.error_retry),
                    new SnackbarManager.SnackbarCallback() {
                        @Override
                        public void onClick(View v) {
                            loadNews(lastLoadedPage, methodBeforeErrorInsert);
                        }
                    });
        }

        swipeRefreshLayout.setRefreshing(false);
    }
}
