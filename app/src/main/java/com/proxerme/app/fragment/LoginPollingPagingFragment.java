package com.proxerme.app.fragment;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.view.View;

import com.proxerme.app.R;
import com.proxerme.app.adapter.PagingAdapter;
import com.proxerme.app.dialog.LoginDialog;
import com.proxerme.app.event.CancelledEvent;
import com.proxerme.app.manager.UserManager;
import com.proxerme.app.util.Utils;
import com.proxerme.library.event.IListEvent;
import com.proxerme.library.event.error.ErrorEvent;
import com.proxerme.library.event.error.LoginErrorEvent;
import com.proxerme.library.event.success.LoginEvent;
import com.proxerme.library.event.success.LogoutEvent;
import com.proxerme.library.interfaces.IdItem;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public abstract class LoginPollingPagingFragment<T extends IdItem & Parcelable,
        A extends PagingAdapter<T, ?>, E extends IListEvent<T>, EE extends ErrorEvent>
        extends PollingPagingFragment<T, A, E, EE> {

    private boolean canLoad;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        canLoad = UserManager.getInstance().isLoggedIn() && !UserManager.getInstance().isWorking();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!canLoad) {
            if (!UserManager.getInstance().isWorking()) {
                showLoginError();
            }

            stopLoading();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLogin(LoginEvent event) {
        if (!canLoad) {
            canLoad = true;

            doLoad(getFirstPage(), true, true);

            if (getParentActivity() != null) {
                getParentActivity().clearMessage();
            }
        } else {
            doLoad(getFirstPage(), true, true);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLogout(LogoutEvent event) {
        canLoad = false;

        cancelRequest();
        clear();
        showLoginError();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoginError(LoginErrorEvent event) {
        showLoginError();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDialogCancelled(CancelledEvent event) {
        if (!UserManager.getInstance().isLoggedIn()) {
            showLoginError();
        }
    }

    @Override
    protected boolean canLoad() {
        return canLoad;
    }

    private void showLoginError() {
        if (getParentActivity() != null) {
            getParentActivity().showMessage(getString(R.string.error_not_logged_in),
                    getString(R.string.error_do_login), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (Utils.areActionsPossible(getParentActivity())) {
                                LoginDialog.show(getParentActivity());
                            }
                        }
                    });
        }
    }


}
