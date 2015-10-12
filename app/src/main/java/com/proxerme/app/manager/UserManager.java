package com.proxerme.app.manager;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerException;
import com.proxerme.library.entity.LoginUser;

import java.util.LinkedList;
import java.util.List;

/**
 * Todo: Describe Class
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
        ProxerConnection.login(user,
                new ProxerConnection.ResultCallback<LoginUser>() {
                    @Override
                    public void onResult(@NonNull LoginUser user) {
                        changeUser(user);

                        for (OnLoginStateListener listener : listeners) {
                            listener.onLogin(user);
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
        ProxerConnection.logout(new ProxerConnection.ResultCallback<Void>() {
            @Override
            public void onResult(@NonNull Void aVoid) {
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
