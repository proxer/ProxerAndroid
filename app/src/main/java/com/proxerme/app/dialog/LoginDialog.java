package com.proxerme.app.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.proxerme.library.entity.LoginUser;
import com.proxerme.library.event.error.LoginErrorEvent;
import com.proxerme.library.event.success.LoginEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * A dialog, which shows a login mask to the user. It also handles the login and shows a ProgressBar
 * to the user.
 *
 * @author Ruben Gees
 */
public class LoginDialog extends DialogFragment {

    private static final String STATE_LOADING = "login_loading";

    ViewGroup root;

    @Bind(R.id.dialog_login_username_container)
    TextInputLayout usernameInputContainer;
    @Bind(R.id.dialog_login_password_container)
    TextInputLayout passwordInputContainer;

    @Bind(R.id.dialog_login_username)
    EditText usernameInput;
    @Bind(R.id.dialog_login_password)
    EditText passwordInput;
    @Bind(R.id.dialog_login_remember)
    CheckBox remember;

    @Bind(R.id.dialog_login_input_container)
    ViewGroup inputContainer;

    @Bind(R.id.dialog_login_progress)
    ProgressBar progress;

    private boolean loading;

    public static void show(@NonNull AppCompatActivity activity) {
        new LoginDialog().show(activity.getSupportFragmentManager(), "login_dialog");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        findViews();
        initViews();

        MaterialDialog.Builder builder = new MaterialDialog.Builder(getContext()).autoDismiss(false)
                .title(R.string.dialog_login_title).positiveText(R.string.dialog_login_go)
                .negativeText(R.string.dialog_cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog,
                                        @NonNull DialogAction dialogAction) {
                        login();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        root = null;
        ButterKnife.unbind(this);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        ProxerConnection.cancel(ProxerTag.LOGIN);
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

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);

        super.onStop();
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onLogin(LoginEvent event) {
        loading = false;

        NotificationRetrievalManager.retrieveMessagesLater(getContext());

        dismiss();
    }

    @Subscribe(sticky = true, priority = 1)
    public void onLoginError(LoginErrorEvent event) {
        EventBus.getDefault().cancelEventDelivery(event);
        EventBus.getDefault().removeStickyEvent(event);

        loading = false;

        handleVisibility();

        //noinspection ThrowableResultOfMethodCallIgnored
        Toast.makeText(getContext(),
                ErrorHandler.getMessageForErrorCode(getContext(),
                        event.getItem().getErrorCode()), Toast.LENGTH_LONG).show();
    }

    private void findViews() {
        root = (ViewGroup) View.inflate(getContext(), R.layout.dialog_login, null);
        ButterKnife.bind(this, root);
    }

    private void initViews() {
        LoginUser user = UserManager.getInstance().getUser();

        if (user != null) {
            usernameInput.setText(user.getUsername());
            passwordInput.setText(user.getPassword());
        }

        passwordInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    login();

                    return true;
                }
                return false;
            }
        });

        usernameInput.addTextChangedListener(new OnTextListener() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                resetError(usernameInputContainer);
            }
        });

        passwordInput.addTextChangedListener(new OnTextListener() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                resetError(passwordInputContainer);
            }
        });
    }

    private void login() {
        if (!loading) {
            String username = usernameInput.getText().toString();
            String password = passwordInput.getText().toString();

            if (checkInput(username, password)) {
                loading = true;
                handleVisibility();

                UserManager.getInstance().login(new LoginUser(username, password),
                        remember.isChecked());
            }
        }
    }

    private void handleVisibility() {
        if (inputContainer != null && progress != null) {
            if (loading) {
                inputContainer.setVisibility(View.INVISIBLE);
                progress.setVisibility(View.VISIBLE);
            } else {
                inputContainer.setVisibility(View.VISIBLE);
                progress.setVisibility(View.INVISIBLE);
            }
        }
    }

    private boolean checkInput(@NonNull String username, @NonNull String password) {
        boolean inputCorrect = true;

        if (TextUtils.isEmpty(username)) {
            inputCorrect = false;

            setError(usernameInputContainer,
                    getContext().getString(R.string.dialog_login_error_no_username));
        }

        if (TextUtils.isEmpty(password)) {
            inputCorrect = false;

            setError(passwordInputContainer,
                    getContext().getString(R.string.dialog_login_error_no_password));
        }
        return inputCorrect;
    }

    private void setError(@NonNull TextInputLayout container, @NonNull String error) {
        container.setError(error);
        container.setErrorEnabled(true);
    }

    private void resetError(@NonNull TextInputLayout container) {
        container.setError(null);
        container.setErrorEnabled(false);
    }

    private static abstract class OnTextListener implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }
}
