package com.proxerme.app.manager;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.proxerme.app.util.helper.StorageHelper;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerException;
import com.proxerme.library.connection.ProxerTag;
import com.proxerme.library.entity.LoginUser;
import com.proxerme.library.event.error.LoginErrorEvent;
import com.proxerme.library.event.error.LogoutErrorEvent;
import com.proxerme.library.event.success.LoginEvent;
import com.proxerme.library.event.success.LogoutEvent;

import org.greenrobot.eventbus.Subscribe;
import org.joda.time.DateTime;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A singleton for managing the user and it's login state.
 *
 * @author Ruben Gees
 */
public class UserManager extends Manager {

    private static final int SAVE_USER = 0;
    private static final int DONT_SAVE_USER = 1;
    private static final int SAME_AS_IS = 2;
    private static final int RELOGIN_THRESHOLD = 30;

    @Nullable
    private LoginUser user;

    private volatile boolean loggedIn = false;
    private volatile boolean loggingIn = false;
    private volatile boolean loggingOut = false;

    @UserSaveMode
    private int saveUser = SAME_AS_IS;

    public UserManager() {
        super();

        user = StorageHelper.getUser();
    }

    @Nullable
    public LoginUser getUser() {
        return user;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public boolean isLoggingIn() {
        return loggingIn;
    }

    public boolean isLoggingOut() {
        return loggingOut;
    }

    public void removeUser() {
        this.user = null;

        StorageHelper.removeUser();
    }

    public void changeUser(@NonNull LoginUser user) {
        this.user = user;

        if (saveUser == SAVE_USER) {
            StorageHelper.setUser(user);
        } else if (saveUser == DONT_SAVE_USER) {
            StorageHelper.removeUser();
        }
    }

    public void login(@NonNull LoginUser user, boolean save) {
        saveUser = save ? SAVE_USER : DONT_SAVE_USER;
        loggingIn = true;
        loggingOut = false;

        ProxerConnection.cancel(ProxerTag.LOGOUT);
        ProxerConnection.login(user).execute();
    }

    public void reLogin() {
        if (user != null) {
            loggingIn = true;
            saveUser = SAME_AS_IS;
            long lastLogin = StorageHelper.getLastLoginTime();

            if (lastLogin <= 0 || new DateTime(lastLogin)
                    .isBefore(new DateTime().minusMinutes(RELOGIN_THRESHOLD))) {
                loggingOut = false;

                ProxerConnection.cancel(ProxerTag.LOGOUT);
                ProxerConnection.login(user).execute();
            } else {
                loggedIn = true;
                loggingIn = false;
            }
        }
    }

    public void reLoginSync() throws ProxerException {
        if (user != null) {
            long lastLogin = StorageHelper.getLastLoginTime();

            if (lastLogin <= 0 || new DateTime(lastLogin)
                    .isBefore(new DateTime().minusMinutes(RELOGIN_THRESHOLD))) {
                loggingOut = false;

                ProxerConnection.cancel(ProxerTag.LOGOUT);

                try {
                    changeUser(ProxerConnection.login(user).executeSynchronized());
                    StorageHelper.setLastLoginTime(System.currentTimeMillis());
                } catch (ProxerException e) {
                    StorageHelper.setLastLoginTime(-1);

                    throw e;
                }
            } else {
                loggedIn = true;
            }
        }
    }

    public void logout() {
        loggingOut = true;
        loggingIn = false;

        ProxerConnection.cancel(ProxerTag.LOGIN);
        ProxerConnection.logout().execute();
    }

    public void cancel() {
        loggingIn = false;
        loggingOut = false;

        ProxerConnection.cancel(ProxerTag.LOGOUT);
        ProxerConnection.cancel(ProxerTag.LOGIN);
    }

    @Subscribe(priority = 1)
    public void onLogin(LoginEvent event) {
        loggedIn = true;
        loggingIn = false;
        changeUser(event.getItem());
        StorageHelper.setLastLoginTime(System.currentTimeMillis());
    }

    @Subscribe(priority = 1)
    public void onLogout(LogoutEvent event) {
        loggedIn = false;
        loggingOut = false;
        removeUser();
        StorageHelper.setLastLoginTime(-1);
    }

    @Subscribe
    public void onLoginError(LoginErrorEvent event) {
        loggingIn = false;

        StorageHelper.setLastLoginTime(-1);
    }

    @Subscribe
    public void onLogoutError(LogoutErrorEvent event) {
        loggingOut = false;

        StorageHelper.setLastLoginTime(-1);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SAVE_USER, DONT_SAVE_USER, SAME_AS_IS})
    private @interface UserSaveMode {

    }
}
