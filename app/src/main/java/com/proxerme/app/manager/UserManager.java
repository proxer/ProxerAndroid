package com.proxerme.app.manager;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.proxerme.app.util.helper.StorageHelper;
import com.proxerme.library.connection.ProxerConnection;
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
    private volatile boolean working = false;

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

    public boolean isWorking() {
        return working;
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
        working = true;

        ProxerConnection.cancel(ProxerTag.LOGOUT);
        ProxerConnection.login(user).execute();
    }

    public void reLogin() {
        if (user != null) {
            working = true;
            saveUser = SAME_AS_IS;
            long lastLogin = StorageHelper.getLastLogin();

            if (lastLogin <= 0 || new DateTime(lastLogin)
                    .isBefore(new DateTime().minusMinutes(RELOGIN_THRESHOLD))) {

                ProxerConnection.cancel(ProxerTag.LOGOUT);
                ProxerConnection.login(user).execute();
            } else {
                loggedIn = true;
                working = false;
            }
        }
    }

    public void logout() {
        working = false;

        ProxerConnection.cancel(ProxerTag.LOGIN);
        ProxerConnection.logout().execute();
    }

    public void cancelLogin() {
        ProxerConnection.cancel(ProxerTag.LOGIN);

        working = false;
    }

    public void cancelLogout() {
        ProxerConnection.cancel(ProxerTag.LOGOUT);

        working = false;
    }

    @Subscribe(priority = 1)
    public void onLogin(LoginEvent event) {
        loggedIn = true;
        working = false;
        changeUser(event.getItem());
        StorageHelper.setLastLogin(System.currentTimeMillis());
    }

    @Subscribe(priority = 1)
    public void onLogout(LogoutEvent event) {
        loggedIn = false;
        working = false;
        removeUser();
        StorageHelper.setLastLogin(-1);
    }

    @Subscribe()
    public void onLoginError(LoginErrorEvent event) {
        working = false;
        StorageHelper.setLastLogin(-1);
    }

    @Subscribe()
    public void onLogoutError(LogoutErrorEvent event) {
        working = false;
        StorageHelper.setLastLogin(-1);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SAVE_USER, DONT_SAVE_USER, SAME_AS_IS})
    private @interface UserSaveMode {

    }
}
