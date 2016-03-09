package com.proxerme.app.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.proxerme.app.R;
import com.proxerme.app.activity.ImageDetailActivity;
import com.proxerme.app.activity.MainActivity;
import com.proxerme.app.adapter.ConferenceAdapter;
import com.proxerme.app.dialog.LoginDialog;
import com.proxerme.app.event.CancelledEvent;
import com.proxerme.app.manager.NotificationManager;
import com.proxerme.app.manager.NotificationRetrievalManager;
import com.proxerme.app.manager.StorageManager;
import com.proxerme.app.manager.UserManager;
import com.proxerme.app.util.MaterialDrawerHelper;
import com.proxerme.app.util.Utils;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerTag;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.entity.Conference;
import com.proxerme.library.event.error.ConferencesErrorEvent;
import com.proxerme.library.event.error.LoginErrorEvent;
import com.proxerme.library.event.success.ConferencesEvent;
import com.proxerme.library.event.success.LoginEvent;
import com.proxerme.library.event.success.LogoutEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * A Fragment, showing a List of Conferences to the user.
 *
 * @author Ruben Gees
 */
public class ConferencesFragment extends PagingFragment<Conference, ConferenceAdapter,
        ConferencesEvent, ConferencesErrorEvent> {

    private static final int POLLING_INTERVAL = 5000;
    @Nullable
    private Handler handler;

    private boolean canLoad;

    @NonNull
    public static ConferencesFragment newInstance() {
        return new ConferencesFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NotificationManager.cancel(getContext(), NotificationManager.MESSAGES_NOTIFICATION);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        View result = super.onCreateView(inflater, container, savedInstanceState);

        if (UserManager.getInstance().isLoggedIn()) {
            canLoad = true;
        } else {
            canLoad = false;

            showLoginError();
            stopLoading();
            stopPolling();
        }

        return result;
    }

    @Override
    public void onResume() {
        super.onStart();

        if (UserManager.getInstance().isLoggedIn()) {
            startPolling();
        }
    }

    @Override
    public void onPause() {
        stopPolling();

        super.onStop();
    }

    @NonNull
    @Override
    protected ConferenceAdapter createAdapter(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return new ConferenceAdapter();
        } else {
            return new ConferenceAdapter(savedInstanceState);
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
    protected void load(@IntRange(from = 1) int page, boolean insert) {
        ProxerConnection.loadConferences(page).execute();
    }

    @Override
    protected void cancelRequest() {
        ProxerConnection.cancel(ProxerTag.CONFERENCES);
    }

    @Override
    protected boolean canLoad() {
        return canLoad;
    }

    @Override
    public void onLoad(@NonNull ConferencesEvent result) {
        super.onLoad(result);

        if (handler == null) {
            startPolling();
        }

        StorageManager.setNewMessages(0);
        StorageManager.resetMessagesInterval();

        if (getContext() != null) {
            NotificationRetrievalManager.retrieveNewsLater(getContext());
        }

        if (getActivity() != null) {
            getDashboardActivity().setBadge(MaterialDrawerHelper.DRAWER_ID_MESSAGES,
                    null);
        }
    }

    @Override
    public void onLoadError(@NonNull ConferencesErrorEvent errorEvent) {
        super.onLoadError(errorEvent);

        stopPolling();
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onLogin(LoginEvent event) {
        if (isEmpty()) {
            canLoad = true;

            doLoad(1, true, true);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onLogout(LogoutEvent event) {
        canLoad = false;

        cancelRequest();
        stopPolling();
        clear();
        showLoginError();
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onLoginError(LoginErrorEvent event) {
        showLoginError();
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onDialogCancelled(CancelledEvent event) {
        if (!UserManager.getInstance().isLoggedIn()) {
            showLoginError();
        }
    }

    private void showLoginError() {
        if (getDashboardActivity() != null) {
            getDashboardActivity().showMessage(getString(R.string.error_not_logged_in),
                    getString(R.string.error_do_login), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (Utils.areActionsPossible(getDashboardActivity())) {
                                LoginDialog.show(getDashboardActivity());
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
