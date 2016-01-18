package com.proxerme.app.manager;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.proxerme.app.event.LoginEvent;
import com.proxerme.app.event.LogoutEvent;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerException;
import com.proxerme.library.connection.ProxerTag;
import com.proxerme.library.entity.LoginUser;

import de.greenrobot.event.EventBus;

/**
 * A singleton for managing the user and it's login state.
 *
 * @author Ruben Gees
 */
public class UserManager {
    private static UserManager ourInstance = new UserManager();
    private LoginUser user;
    private boolean loggedIn = false;

    private UserManager() {
        user = StorageManager.getUser();
    }

    @NonNull
    public static UserManager getInstance() {
        return ourInstance;
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
        ProxerConnection.login(user).execute(new ProxerConnection.ResultCallback<LoginUser>() {
            @Override
            public void onResult(LoginUser loginUser) {
                loggedIn = true;
                changeUser(loginUser);

                EventBus.getDefault().post(new LoginEvent(null));
            }

            @Override
            public void onError(@NonNull ProxerException e) {
                EventBus.getDefault().post(new LoginEvent(e.getErrorCode()));
            }
        });
    }

    public void logout(){
        ProxerConnection.cancel(ProxerTag.LOGIN);
        ProxerConnection.logout().execute(new ProxerConnection.ResultCallback<Void>() {
            @Override
            public void onResult(Void aVoid) {
                removeUser();
                loggedIn = false;

                EventBus.getDefault().post(new LogoutEvent(null));
            }

            @Override
            public void onError(@NonNull ProxerException e) {
                EventBus.getDefault().post(new LogoutEvent(e.getErrorCode()));
            }
        });
    }
}
