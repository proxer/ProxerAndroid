package com.proxerme.app.manager;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerTag;
import com.proxerme.library.entity.LoginUser;
import com.proxerme.library.event.success.LoginEvent;
import com.proxerme.library.event.success.LogoutEvent;

import de.greenrobot.event.EventBus;

/**
 * A singleton for managing the user and it's login state.
 *
 * @author Ruben Gees
 */
public class UserManager {
    private static UserManager instance;
    private LoginUser user;
    private boolean loggedIn = false;

    private UserManager() {
        user = StorageManager.getUser();

        EventBus.getDefault().registerSticky(this, 1);
    }

    @NonNull
    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }

        return instance;
    }

    public boolean hasUser() {
        return user != null;
    }

    @Nullable
    public LoginUser getUser() {
        return user;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void removeUser() {
        this.user = null;
        StorageManager.removeUser();
    }

    public void changeUser(@NonNull LoginUser user) {
        this.user = user;
        StorageManager.setUser(user);
    }

    public void notifyLoggedOut() {
        loggedIn = false;
    }

    public void login(@NonNull LoginUser user) {
        ProxerConnection.cancel(ProxerTag.LOGOUT);
        ProxerConnection.login(user).execute();
    }

    public void logout(){
        ProxerConnection.cancel(ProxerTag.LOGIN);
        ProxerConnection.logout().execute();
    }

    public void onEvent(LoginEvent event) {
        EventBus.getDefault().removeStickyEvent(event);

        loggedIn = true;
        changeUser(event.getItem());
    }

    public void onEvent(LogoutEvent event) {
        EventBus.getDefault().removeStickyEvent(event);

        removeUser();
        loggedIn = false;
    }

    public void destroy() {
        EventBus.getDefault().unregister(this);
    }
}
