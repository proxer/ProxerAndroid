package com.proxerme.app.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.proxerme.app.R;
import com.proxerme.app.event.CancelledEvent;
import com.proxerme.app.util.ErrorHandler;
import com.proxerme.library.event.error.LogoutErrorEvent;
import com.proxerme.library.event.success.LogoutEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Dialog, which handles the logout of a user.
 *
 * @author Ruben Gees
 */
public class LogoutDialog extends MainDialog {

    private static final String STATE_LOADING = "logout_loading";

    ViewGroup root;

    @Bind(R.id.dialog_logout_progress)
    ProgressBar progress;

    private boolean loading;

    public static void show(@NonNull AppCompatActivity activity) {
        new LogoutDialog().show(activity.getSupportFragmentManager(), "logout_dialog");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
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
                }).customView(initViews(), true);

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
        ButterKnife.unbind(this);
        root = null;

        super.onDestroyView();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        getMainApplication().getUserManager().cancelLogout();
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLogout(LogoutEvent event) {
        loading = false;

        dismiss();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLogoutError(LogoutErrorEvent event) {
        loading = false;

        handleVisibility();

        //noinspection ThrowableResultOfMethodCallIgnored
        Toast.makeText(getContext(),
                ErrorHandler.getMessageForErrorCode(getContext(),
                        event.getItem()), Toast.LENGTH_LONG).show();
    }

    private View initViews() {
        root = (ViewGroup) View.inflate(getContext(), R.layout.dialog_logout, null);

        ButterKnife.bind(this, root);

        return root;
    }

    private void logout() {
        if (!loading) {
            loading = true;
            handleVisibility();

            getMainApplication().getUserManager().logout();
        }
    }
}
