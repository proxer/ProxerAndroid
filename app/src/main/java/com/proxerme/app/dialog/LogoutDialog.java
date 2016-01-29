package com.proxerme.app.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.proxerme.app.R;
import com.proxerme.app.event.CancelledEvent;
import com.proxerme.app.manager.NotificationRetrievalManager;
import com.proxerme.app.manager.UserManager;
import com.proxerme.app.util.ErrorHandler;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerTag;
import com.proxerme.library.event.error.LogoutErrorEvent;
import com.proxerme.library.event.success.LogoutEvent;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

/**
 * Dialog, which handles the logout of a user.
 *
 * @author Ruben Gees
 */
public class LogoutDialog extends DialogFragment {

    private static final String STATE_LOADING = "login_loading";

    ViewGroup root;

    @Bind(R.id.dialog_logout_progress)
    ProgressBar progress;

    private boolean loading;

    @NonNull
    public static LogoutDialog newInstance() {
        return new LogoutDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        findViews();

        MaterialDialog.Builder builder = new MaterialDialog.Builder(getContext()).autoDismiss(false)
                .title(R.string.dialog_logout_title).positiveText(R.string.dialog_logout_go)
                .negativeText(R.string.dialog_cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog,
                                        @NonNull DialogAction dialogAction) {
                        logout();
                    }
                }).onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog,
                                        @NonNull DialogAction dialogAction) {
                        materialDialog.cancel();
                    }
                }).customView(root, true);

        return builder.build();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            loading = savedInstanceState.getBoolean(STATE_LOADING);
        }

        handleVisibility();
    }

    private void handleVisibility() {
        if (loading) {
            progress.setVisibility(View.VISIBLE);
        } else {
            progress.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        root = null;
        ButterKnife.unbind(this);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        ProxerConnection.cancel(ProxerTag.LOGOUT);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        EventBus.getDefault().post(new CancelledEvent());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(STATE_LOADING, loading);
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().registerSticky(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);

        super.onStop();
    }

    public void onEvent(LogoutEvent event) {
        loading = false;

        NotificationRetrievalManager.cancelMessagesRetrieval(getContext());

        dismiss();
    }

    public void onEventMainThread(LogoutErrorEvent event) {
        EventBus.getDefault().removeStickyEvent(event);

        loading = false;

        handleVisibility();

        //noinspection ThrowableResultOfMethodCallIgnored
        Toast.makeText(getContext(),
                ErrorHandler.getMessageForErrorCode(getContext(),
                        event.getItem().getErrorCode()), Toast.LENGTH_LONG).show();
    }

    private void findViews() {
        root = (ViewGroup) View.inflate(getContext(), R.layout.dialog_logout, null);
        ButterKnife.bind(this, root);
    }

    private void logout() {
        if (!loading) {
            loading = true;
            handleVisibility();

            UserManager.getInstance().logout();
        }
    }
}
