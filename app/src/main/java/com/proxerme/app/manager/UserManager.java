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

    private volatile boolean loggedIn = false;
    private volatile boolean working = false;

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

    public boolean isWorking() {
        return working;
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
        working = true;

        ProxerConnection.cancel(ProxerTag.LOGOUT);
        ProxerConnection.login(user).execute();
    }

    public void reLogin() {
        if (user != null) {
            saveUser = SAME_AS_IS;
            working = true;

            ProxerConnection.cancel(ProxerTag.LOGOUT);
            ProxerConnection.login(user).execute();
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

    @Subscribe(sticky = true, priority = 1)
    public void onLogin(LoginEvent event) {
        loggedIn = true;
        working = false;
        changeUser(event.getItem());

        EventBus.getDefault().removeStickyEvent(event);
    }

    @Subscribe(sticky = true, priority = 1)
    public void onLogout(LogoutEvent event) {
        loggedIn = false;
        working = false;
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
