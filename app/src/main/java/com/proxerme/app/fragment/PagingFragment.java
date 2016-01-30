package com.proxerme.app.fragment;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.proxerme.app.util.PagingHelper;
import com.proxerme.app.util.Utils;
import com.proxerme.library.connection.ProxerException;
import com.proxerme.library.event.IListEvent;
import com.proxerme.library.event.error.ErrorEvent;
import com.proxerme.library.interfaces.IdItem;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

/**
 * An abstract Fragment, managing page based Lists of items.
 *
 * @author Ruben Gees
 */
public abstract class PagingFragment<T extends IdItem & Parcelable, A extends PagingAdapter<T, ?>,
        E extends IListEvent<T>, EE extends ErrorEvent> extends DashboardFragment {

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
    private boolean lastMethodInsert = false;
    private boolean endReached = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_paging, container, false);
        ButterKnife.bind(this, root);

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(
                getActivity() == null ? 1 : Utils.calculateSpanAmount(getActivity()),
                StaggeredGridLayoutManager.VERTICAL);

        list.setHasFixedSize(true);
        list.setLayoutManager(layoutManager);
        list.addOnScrollListener(new EndlessRecyclerOnScrollListener(layoutManager) {
            @Override
            public void onLoadMore() {
                if (!loading && !endReached) {
                    doLoad(currentPage, false, true);
                }
            }
        });

        swipeRefreshLayout.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefreshLayout.setSize(SwipeRefreshLayout.DEFAULT);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                doLoad(1, true, true);
            }
        });

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter = createAdapter(savedInstanceState);

        if (savedInstanceState != null) {
            loading = savedInstanceState.getBoolean(STATE_LOADING);
            currentPage = savedInstanceState.getInt(STATE_CURRENT_PAGE);
            lastLoadedPage = savedInstanceState.getInt(STATE_LAST_LOADED_PAGE);
            currentErrorMessage = savedInstanceState.getString(STATE_ERROR_MESSAGE);
            lastMethodInsert = savedInstanceState.getBoolean(STATE_METHOD_BEFORE_ERROR);
            endReached = savedInstanceState.getBoolean(STATE_END_REACHED);
        }

        configAdapter(adapter);
        list.setAdapter(adapter);

        if (savedInstanceState == null) {
            doLoad(currentPage, false, true);
        } else if (currentErrorMessage != null) {
            showError();
        } else if (loading) {
            swipeRefreshLayout.setRefreshing(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.unbind(this);
        root = null;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().registerSticky(this);
        }
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);

        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (getActivity() != null && !getActivity().isChangingConfigurations()) {
            cancelRequest();
        }

        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        adapter.saveInstanceState(outState);
        outState.putBoolean(STATE_LOADING, loading);
        outState.putInt(STATE_CURRENT_PAGE, currentPage);
        outState.putInt(STATE_LAST_LOADED_PAGE, lastLoadedPage);
        outState.putString(STATE_ERROR_MESSAGE, currentErrorMessage);
        outState.putBoolean(STATE_METHOD_BEFORE_ERROR, lastMethodInsert);
        outState.putBoolean(STATE_END_REACHED, endReached);
    }

    protected void doLoad(@IntRange(from = 1) final int page, final boolean insert,
                          final boolean showProgress) {
        if (!isLoading() && canLoad()) {
            lastLoadedPage = page;
            loading = true;
            currentErrorMessage = null;
            lastMethodInsert = insert;

            if (showProgress) {
                swipeRefreshLayout.setRefreshing(true);
            }

            if (getDashboardActivity() != null) {
                getDashboardActivity().clearMessage();
            }

            load(page, insert);
        } else {
            //Possible if the user used swipe to refresh
            stopLoading();
        }
    }

    public void onEventMainThread(E result) {
        EventBus.getDefault().removeStickyEvent(result);

        if ((result.getItem()).isEmpty()) {
            if (!lastMethodInsert) {
                endReached = true;
            }
        } else {
            if (!lastMethodInsert) {
                currentPage++;
            }
        }

        stopLoading();
        handleResult(result.getItem(), lastMethodInsert);
    }

    public void onEventMainThread(EE errorResult) {
        //noinspection ThrowableResultOfMethodCallIgnored
        ProxerException exception = errorResult.getItem();

        if (exception.getErrorCode() == ProxerException.ErrorCodes.PROXER) {
            currentErrorMessage = exception.getMessage();
        } else {
            currentErrorMessage = ErrorHandler.getMessageForErrorCode(getContext(),
                    exception.getErrorCode());
        }

        stopLoading();
        showError();
    }

    private void handleResult(List<T> result, boolean insert) {
        if (insert) {
            int offset = adapter.insertAtStart(result);

            if (offset == PagingHelper.OFFSET_TOO_LARGE || offset > 0) {
                scrollToTop();
            }
        } else {
            adapter.append(result);
        }
    }

    protected void stopLoading() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }

        loading = false;
    }

    private void showError() {
        if (getDashboardActivity() != null) {
            getDashboardActivity().showMessage(currentErrorMessage,
                    getContext().getString(R.string.error_retry), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            doLoad(lastLoadedPage, lastMethodInsert, true);
                        }
                    });
        }
    }

    protected boolean isLoading() {
        return loading;
    }

    protected boolean isEmpty() {
        return adapter != null && adapter.isEmpty();
    }

    protected void clear() {
        if (adapter != null) {
            adapter.clear();
        }
    }

    protected boolean canLoad() {
        return true;
    }

    protected abstract A createAdapter(Bundle savedInstanceState);

    protected abstract void load(@IntRange(from = 1) final int page, final boolean insert);

    protected abstract void cancelRequest();

    protected void configAdapter(@NonNull A adapter) {

    }

    protected void scrollToTop() {
        if (list != null) {
            list.smoothScrollToPosition(0);
        }
    }
}
