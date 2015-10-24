package com.proxerme.app.fragment;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.proxerme.app.R;
import com.proxerme.app.adapter.PagingAdapter;
import com.proxerme.app.util.EndlessRecyclerOnScrollListener;
import com.proxerme.app.util.ErrorHandler;
import com.proxerme.app.util.SnackbarManager;
import com.proxerme.app.util.Utils;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerException;
import com.proxerme.library.interfaces.IdItem;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * An abstract Fragment, managing page based Lists of items.
 *
 * @author Ruben Gees
 */
public abstract class PagingFragment<T extends IdItem & Parcelable, A extends PagingAdapter<T, ?>>
        extends DashboardFragment {

    private static final String STATE_LOADING = "paging_loading";
    private static final String STATE_METHOD_BEFORE_ERROR = "paging_method_before_error";
    private static final String STATE_CURRENT_PAGE = "paging_current_page";
    private static final String STATE_LAST_LOADED_PAGE = "paging_last_loaded_page";
    private static final String STATE_ERROR_MESSAGE = "paging_error_message";
    private static final String STATE_END_REACHED = "paging_end_reached";

    View root;
    @Bind(R.id.fragment_paging_list_container)
    SwipeRefreshLayout swipeRefreshLayout;
    @Bind(R.id.fragment_paging_list)
    RecyclerView list;

    private A adapter;

    private boolean loading = false;
    private int currentPage = 1;
    private int lastLoadedPage = 1;
    private String currentErrorMessage;
    private boolean methodBeforeErrorInsert = false;
    private boolean endReached = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = getAdapter(savedInstanceState);

        if (savedInstanceState != null) {
            loading = savedInstanceState.getBoolean(STATE_LOADING);
            currentPage = savedInstanceState.getInt(STATE_CURRENT_PAGE);
            lastLoadedPage = savedInstanceState.getInt(STATE_LAST_LOADED_PAGE);
            currentErrorMessage = savedInstanceState.getString(STATE_ERROR_MESSAGE);
            methodBeforeErrorInsert = savedInstanceState.getBoolean(STATE_METHOD_BEFORE_ERROR);
            endReached = savedInstanceState.getBoolean(STATE_END_REACHED);
        }

        configAdapter(adapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_paging, container, false);
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
                if (!loading && !endReached) {
                    doLoad(currentPage, false);
                }
            }
        });

        swipeRefreshLayout.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                doLoad(1, true);
            }
        });

        if (savedInstanceState == null) {
            doLoad(currentPage, false);
        } else if (currentErrorMessage != null) {
            showError();
        } else if (loading) {
            doLoad(currentPage, methodBeforeErrorInsert);
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
    public void onDestroy() {
        super.onDestroy();

        cancelRequest();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        adapter.saveInstanceState(outState);
        outState.putBoolean(STATE_LOADING, loading);
        outState.putInt(STATE_CURRENT_PAGE, currentPage);
        outState.putInt(STATE_LAST_LOADED_PAGE, lastLoadedPage);
        outState.putString(STATE_ERROR_MESSAGE, currentErrorMessage);
        outState.putBoolean(STATE_METHOD_BEFORE_ERROR, methodBeforeErrorInsert);
        outState.putBoolean(STATE_END_REACHED, endReached);
    }

    private void doLoad(@IntRange(from = 1) final int page, final boolean insert) {
        lastLoadedPage = page;
        loading = true;
        currentErrorMessage = null;

        swipeRefreshLayout.setRefreshing(true);

        load(page, insert, new ProxerConnection.ResultCallback<List<T>>() {
            @Override
            public void onResult(List<T> result) {
                if (result.isEmpty()) {
                    if (!insert) {
                        endReached = true;
                    }
                } else {
                    if (!insert) {
                        currentPage++;
                    }
                }

                loading = false;

                handleResult(result, insert);

                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onError(@NonNull ProxerException exception) {
                if (exception.getErrorCode() == ProxerException.ErrorCodes.PROXER) {
                    currentErrorMessage = exception.getMessage();
                } else {
                    currentErrorMessage = ErrorHandler.getMessageForErrorCode(getContext(),
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

    private void handleResult(List<T> result, boolean insert) {
        if (insert) {
            adapter.insertAtStart(result);
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
                            doLoad(lastLoadedPage, methodBeforeErrorInsert);
                        }
                    });
        }

        swipeRefreshLayout.setRefreshing(false);
    }

    protected abstract A getAdapter(Bundle savedInstanceState);

    protected abstract void load(@IntRange(from = 1) final int page, final boolean insert,
                                 @NonNull ProxerConnection.ResultCallback<List<T>> callback);

    protected abstract void cancelRequest();

    protected void configAdapter(@NonNull A adapter) {

    }

}
