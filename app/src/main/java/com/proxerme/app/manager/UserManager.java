package com.proxerme.app.manager;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerException;
import com.proxerme.library.connection.ProxerTag;
import com.proxerme.library.entity.LoginUser;

import java.util.LinkedList;
import java.util.List;

/**
 * A singleton for managing the user and it's login state.
 *
 * @author Ruben Gees
 */
public class UserManager {
    private static UserManager ourInstance = new UserManager();
    private LoginUser user;
    private List<OnLoginStateListener> listeners;

    private UserManager() {
        user = StorageManager.getUser();
        listeners = new LinkedList<>();
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

    public void removeUser() {
        this.user = null;
        StorageManager.removeUser();
    }

    public void changeUser(@NonNull LoginUser user) {
        this.user = user;
        StorageManager.setUser(user);
    }

    public void login(@NonNull LoginUser user) {
        ProxerConnection.cancel(ProxerTag.LOGOUT);
        ProxerConnection.login(user).execute(new ProxerConnection.ResultCallback<LoginUser>() {
            @Override
            public void onResult(LoginUser loginUser) {
                changeUser(loginUser);

                for (OnLoginStateListener listener : listeners) {
                    listener.onLogin(loginUser);
                }
            }

            @Override
            public void onError(@NonNull ProxerException e) {
                for (OnLoginStateListener listener : listeners) {
                    listener.onLoginFailed(e);
                }
            }
        });
    }

    public void logout(){
        ProxerConnection.cancel(ProxerTag.LOGIN);
        ProxerConnection.logout().execute(new ProxerConnection.ResultCallback<Void>() {
            @Override
            public void onResult(Void aVoid) {
                removeUser();

                for (OnLoginStateListener listener : listeners) {
                    listener.onLogout();
                }
            }

            @Override
            public void onError(@NonNull ProxerException e) {
                for (OnLoginStateListener listener : listeners) {
                    listener.onLogoutFailed(e);
                }
            }
        });
    }

    public void addOnLoginStateListener(@NonNull OnLoginStateListener listener){
        this.listeners.add(listener);
    }

    public abstract static class OnLoginStateListener{
        public void onLogin(@NonNull LoginUser user){

        }

        public void onLogout(){

        }

        public void onLoginFailed(@NonNull ProxerException exception){

        }

        public void onLogoutFailed(@NonNull ProxerException exception){

        }
    }
}
