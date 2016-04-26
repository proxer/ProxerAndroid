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
import android.widget.TextView;

import com.proxerme.app.R;
import com.proxerme.app.adapter.PagingAdapter;
import com.proxerme.app.util.ErrorHandler;
import com.proxerme.app.util.Utils;
import com.proxerme.app.util.listener.EndlessRecyclerOnScrollListener;
import com.proxerme.app.util.listener.NotificationRecyclerOnScrollListener;
import com.proxerme.library.connection.ProxerException;
import com.proxerme.library.event.IListEvent;
import com.proxerme.library.event.error.ErrorEvent;
import com.proxerme.library.interfaces.IdItem;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static com.proxerme.library.connection.ProxerException.ERROR_PROXER;

/**
 * An abstract Fragment, managing page based Lists of items.
 *
 * @author Ruben Gees
 */
public abstract class PagingFragment<T extends IdItem & Parcelable, A extends PagingAdapter<T, ?>,
        E extends IListEvent<T>, EE extends ErrorEvent> extends MainFragment {

    private static final String STATE_LOADING = "paging_loading";
    private static final String STATE_METHOD_BEFORE_ERROR = "paging_method_before_error";
    private static final String STATE_CURRENT_PAGE = "paging_current_page";
    private static final String STATE_LAST_LOADED_PAGE = "paging_last_loaded_page";
    private static final String STATE_ERROR_MESSAGE = "paging_error_message";
    private static final String STATE_END_REACHED = "paging_end_reached";
    private static final String STATE_NEW_ITEMS = "paging_new_items";
    private static final String STATE_SHOW_LOADING = "paging_show_loading";

    protected A adapter;

    View root;
    @BindView(R.id.fragment_paging_list_container)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.fragment_paging_list)
    RecyclerView list;

    @BindView(R.id.fragment_paging_notification_container)
    ViewGroup notificationContainer;
    @BindView(R.id.fragment_paging_notification)
    TextView notification;

    StaggeredGridLayoutManager layoutManager;

    private boolean loading = false;
    private boolean showLoading;

    private int currentPage = getFirstPage();
    private int lastLoadedPage = getFirstPage();

    private String currentErrorMessage;

    private boolean lastMethodInsert = false;
    private boolean endReached = false;
    private boolean firstLoad;

    private int newItems = 0;

    private Unbinder unbinder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = createAdapter(savedInstanceState);

        if (savedInstanceState != null) {
            loading = savedInstanceState.getBoolean(STATE_LOADING);
            showLoading = savedInstanceState.getBoolean(STATE_SHOW_LOADING);
            currentPage = savedInstanceState.getInt(STATE_CURRENT_PAGE);
            lastLoadedPage = savedInstanceState.getInt(STATE_LAST_LOADED_PAGE);
            currentErrorMessage = savedInstanceState.getString(STATE_ERROR_MESSAGE);
            lastMethodInsert = savedInstanceState.getBoolean(STATE_METHOD_BEFORE_ERROR);
            endReached = savedInstanceState.getBoolean(STATE_END_REACHED);
            newItems = savedInstanceState.getInt(STATE_NEW_ITEMS);
            firstLoad = false;
        } else {
            showLoading = true;
            firstLoad = true;
        }

        configAdapter(adapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        root = inflateLayout(inflater, container, savedInstanceState);
        unbinder = ButterKnife.bind(this, root);

        layoutManager = new StaggeredGridLayoutManager(
                getActivity() == null ? 1 : Utils.calculateSpanAmount(getActivity()),
                StaggeredGridLayoutManager.VERTICAL);

        configLayoutManager(layoutManager);

        list.setHasFixedSize(true);
        list.setLayoutManager(layoutManager);
        list.setAdapter(adapter);
        list.addOnScrollListener(new EndlessRecyclerOnScrollListener(layoutManager) {
            @Override
            public void onLoadMore() {
                if (!endReached) {
                    doLoad(currentPage, false, true);
                }
            }
        });

        list.addOnScrollListener(new NotificationRecyclerOnScrollListener(layoutManager) {
            @Override
            public void onStartReached() {
                notificationContainer.setVisibility(View.GONE);
            }
        });

        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                doLoad(getFirstPage(), true, true);
            }
        });

        if (newItems > 0) {
            notificationContainer.setVisibility(View.VISIBLE);
        } else {
            notificationContainer.setVisibility(View.GONE);
        }

        return root;
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        root = null;

        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (firstLoad) {
            doLoad(currentPage, false, true);
        } else if (currentErrorMessage != null) {
            showError();
        } else if (loading) {
            startLoading(showLoading);
        }
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
        outState.putBoolean(STATE_SHOW_LOADING, showLoading);
        outState.putInt(STATE_CURRENT_PAGE, currentPage);
        outState.putInt(STATE_LAST_LOADED_PAGE, lastLoadedPage);
        outState.putString(STATE_ERROR_MESSAGE, currentErrorMessage);
        outState.putBoolean(STATE_METHOD_BEFORE_ERROR, lastMethodInsert);
        outState.putBoolean(STATE_END_REACHED, endReached);
        outState.putInt(STATE_NEW_ITEMS, newItems);
    }

    @OnClick(R.id.fragment_paging_notification_container)
    void onNotificationClick() {
        list.smoothScrollToPosition(0);

        notificationContainer.setVisibility(View.GONE);
    }

    protected void doLoad(@IntRange(from = 0) final int page, final boolean insert,
                          final boolean showProgress) {
        firstLoad = false;

        if (!isLoading()) {
            if (canLoad() && currentErrorMessage == null) {
                lastLoadedPage = page;
                lastMethodInsert = insert;

                startLoading(showProgress);

                if (getParentActivity() != null) {
                    getParentActivity().clearMessage();
                }

                load(page, insert);
            } else {
                stopLoading();
            }
        }
    }

    protected final void handleResult(E result) {
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

    protected void handleError(EE errorResult) {
        //noinspection ThrowableResultOfMethodCallIgnored
        ProxerException exception = errorResult.getItem();

        if (exception.getErrorCode() == ERROR_PROXER) {
            currentErrorMessage = exception.getMessage();
        } else {
            currentErrorMessage = ErrorHandler.getMessageForErrorCode(getContext(), exception);
        }
        stopLoading();
        showError();
    }

    protected void handleResult(List<T> result, boolean insert) {
        if (insert) {
            int[] itemPositions = new int[layoutManager.getSpanCount()];
            layoutManager.findFirstVisibleItemPositions(itemPositions);
            boolean wasAtStart = itemPositions.length > 0 && itemPositions[0] == 0;

            newItems = adapter.insertAtStart(result);

            //noinspection ConstantConditions
            if (newItems > 0) {
                if (wasAtStart) {
                    list.smoothScrollToPosition(0);
                } else {
                    notificationContainer.setVisibility(View.VISIBLE);

                    notification.setText(getNotificationText(newItems));
                }
            }

        } else {
            adapter.append(result);
        }
    }

    protected final void startLoading(boolean show) {
        showLoading = show;

        if (show) {
            swipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    swipeRefreshLayout.setRefreshing(true);
                }
            });
        }

        loading = true;
    }

    protected final void stopLoading() {
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        loading = false;
    }

    private void showError() {
        if (getParentActivity() != null) {
            getParentActivity().showMessage(currentErrorMessage,
                    getContext().getString(R.string.error_retry), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            currentErrorMessage = null;

                            doLoad(lastLoadedPage, lastMethodInsert, true);
                        }
                    });
        }
    }

    protected boolean isLoading() {
        return loading;
    }

    protected void clear() {
        if (adapter != null) {
            adapter.clear();
        }
    }

    protected boolean canLoad() {
        return true;
    }

    protected boolean isFirstLoad() {
        return firstLoad;
    }

    protected abstract A createAdapter(Bundle savedInstanceState);

    protected abstract void load(@IntRange(from = 1) final int page, final boolean insert);

    protected abstract void cancelRequest();

    @NonNull
    protected abstract String getNotificationText(int amount);

    protected View inflateLayout(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_paging, container, false);
    }

    protected void configAdapter(@NonNull A adapter) {

    }

    protected void configLayoutManager(@NonNull StaggeredGridLayoutManager layoutManager) {

    }

    protected int getFirstPage() {
        return 1;
    }
}
