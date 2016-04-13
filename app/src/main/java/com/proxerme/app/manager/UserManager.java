package com.proxerme.app.manager;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerTag;
import com.proxerme.library.entity.LoginUser;
import com.proxerme.library.event.success.LoginEvent;
import com.proxerme.library.event.success.LogoutEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A singleton for managing the user and it's login state.
 *
 * @author Ruben Gees
 */
public class UserManager {
    private static final int SAVE_USER = 0;
    private static final int DONT_SAVE_USER = 1;
    private static final int SAME_AS_IS = 2;

    private static UserManager instance;
    private LoginUser user;

    private boolean loggedIn = false;
    private boolean loggingIn = false;

    @UserSaveMode
    private int saveUser = SAME_AS_IS;

    private UserManager() {
        user = StorageManager.getUser();

        EventBus.getDefault().register(this);
    }

    @NonNull
    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }

        return instance;
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

    public void removeUser() {
        this.user = null;
        StorageManager.removeUser();
    }

    public void changeUser(@NonNull LoginUser user) {
        this.user = user;

        if (saveUser == SAVE_USER) {
            StorageManager.setUser(user);
        } else if (saveUser == DONT_SAVE_USER) {
            StorageManager.removeUser();
        }
    }

    public void notifyLoggedOut() {
        loggedIn = false;

        EventBus.getDefault().post(new LogoutEvent());
    }

    public void login(@NonNull LoginUser user, boolean save) {
        saveUser = save ? SAVE_USER : DONT_SAVE_USER;
        loggingIn = true;

        ProxerConnection.cancel(ProxerTag.LOGOUT);
        ProxerConnection.login(user).execute();
    }

    public void reLogin() {
        if (user != null) {
            saveUser = SAME_AS_IS;
            loggingIn = true;

            ProxerConnection.cancel(ProxerTag.LOGOUT);
            ProxerConnection.login(user).execute();
        }
    }

    public void logout() {
        loggingIn = false;

        ProxerConnection.cancel(ProxerTag.LOGIN);
        ProxerConnection.logout().execute();
    }

    @Subscribe(sticky = true, priority = 1)
    public void onLogin(LoginEvent event) {
        loggedIn = true;
        loggingIn = false;
        changeUser(event.getItem());

        EventBus.getDefault().removeStickyEvent(event);
    }

    @Subscribe(sticky = true, priority = 1)
    public void onLogout(LogoutEvent event) {
        loggedIn = false;
        loggingIn = false;
        removeUser();

        EventBus.getDefault().removeStickyEvent(event);
    }

    public void destroy() {
        EventBus.getDefault().unregister(this);

        instance = null;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SAVE_USER, DONT_SAVE_USER, SAME_AS_IS})
    private @interface UserSaveMode {

    }
}
