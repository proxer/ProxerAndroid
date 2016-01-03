package com.proxerme.app.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.proxerme.app.R;
import com.proxerme.app.activity.DashboardActivity;
import com.proxerme.app.activity.ImageDetailActivity;
import com.proxerme.app.activity.MainActivity;
import com.proxerme.app.adapter.ConferenceAdapter;
import com.proxerme.app.manager.NotificationManager;
import com.proxerme.app.manager.NotificationRetrievalManager;
import com.proxerme.app.manager.StorageManager;
import com.proxerme.app.manager.UserManager;
import com.proxerme.app.util.MaterialDrawerHelper;
import com.proxerme.app.util.SnackbarManager;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerException;
import com.proxerme.library.connection.ProxerTag;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.entity.Conference;
import com.proxerme.library.entity.LoginUser;

import java.util.List;

/**
 * A Fragment, showing a List of Conferences to the user.
 *
 * @author Ruben Gees
 */
public class ConferencesFragment extends PagingFragment<Conference, ConferenceAdapter> {

    private static final int POLLING_INTERVAL = 5000;
    private Handler handler;

    private UserManager.OnLoginStateListener onLoginStateListener =
            new UserManager.OnLoginStateListener() {
                @Override
                public void onLogin(@NonNull LoginUser user) {
                    doLoad(1, true, true);
                }

                @Override
                public void onLogout() {
                    cancelRequest();
                    stopPolling();
                }
            };

    @NonNull
    public static ConferencesFragment newInstance() {
        return new ConferencesFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NotificationManager.cancel(getContext(), NotificationManager.MESSAGES_NOTIFICATION);
        UserManager.getInstance().addOnLoginStateListener(onLoginStateListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = super.onCreateView(inflater, container, savedInstanceState);

        if (savedInstanceState != null) {
            showErrorIfNecessary();
        }

        return result;
    }

    @Override
    public void onStart() {
        super.onStart();

        startPolling();
    }

    @Override
    public void onStop() {
        super.onStop();

        stopPolling();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        UserManager.getInstance().removeOnLoginStateListener(onLoginStateListener);
    }

    @Override
    protected ConferenceAdapter getAdapter(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return new ConferenceAdapter();
        } else {
            return new ConferenceAdapter(savedInstanceState);
        }
    }

    @Override
    protected void load(@IntRange(from = 1) int page, boolean insert,
                        @NonNull final ProxerConnection.ResultCallback<List<Conference>> callback) {
        if (UserManager.getInstance().hasUser()) {
            ProxerConnection.loadConferences(page).execute(new ProxerConnection
                    .ResultCallback<List<Conference>>() {
                @Override
                public void onResult(List<Conference> conferences) {
                    callback.onResult(conferences);

                    if (handler == null) {
                        startPolling();
                    }

                    StorageManager.setNewMessages(0);
                    StorageManager.resetMessagesInterval();
                    NotificationRetrievalManager.retrieveNewsLater(getContext());

                    if (getActivity() != null) {
                        getDashboardActivity().setBadge(MaterialDrawerHelper.DRAWER_ID_MESSAGES, null);
                    }
                }

                @Override
                public void onError(@NonNull ProxerException exception) {
                    callback.onError(exception);

                    stopPolling();
                }
            });
        } else {
            showLoginError();
            stopLoading();
            stopPolling();
        }
    }

    @Override
    protected void configAdapter(@NonNull ConferenceAdapter adapter) {
        adapter.setOnConferenceInteractionListener(new ConferenceAdapter
                .OnConferenceInteractionListener() {
            @Override
            public void onConferenceClick(@NonNull View v, @NonNull Conference conference) {
                if (getActivity() != null) {
                    ((MainActivity) getActivity())
                            .showPage(UrlHolder.getConferenceUrl(conference.getId()));
                }
            }

            @Override
            public void onConferenceImageClick(@NonNull View v, @NonNull Conference conference) {
                if (!TextUtils.isEmpty(conference.getImageId())) {
                    ImageDetailActivity.navigateTo(getActivity(), (ImageView) v,
                            UrlHolder.getUserImage(conference.getImageId()));
                }
            }
        });
    }

    @Override
    protected void cancelRequest() {
        ProxerConnection.cancel(ProxerTag.CONFERENCES);
    }

    @Override
    public void showErrorIfNecessary() {
        super.showErrorIfNecessary();

        if (!UserManager.getInstance().hasUser()) {
            showLoginError();
        }
    }

    private void showLoginError() {
        if (!SnackbarManager.isShowing()) {
            SnackbarManager.show(Snackbar.make(root, R.string.error_not_logged_in,
                    Snackbar.LENGTH_INDEFINITE),
                    getContext().getString(R.string.error_do_login),
                    new SnackbarManager.SnackbarCallback() {
                        @Override
                        public void onClick(View v) {
                            DashboardActivity activity = getDashboardActivity();

                            if (activity != null && !activity.isDestroyedCompat()) {
                                activity.showLoginDialog();
                            }
                        }
                    });
        }
    }

    private void startPolling() {
        handler = new Handler();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doLoad(1, true, false);

                if (handler != null) {
                    handler.postDelayed(this, POLLING_INTERVAL);
                }
            }
        }, POLLING_INTERVAL);
    }

    private void stopPolling() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }
}
