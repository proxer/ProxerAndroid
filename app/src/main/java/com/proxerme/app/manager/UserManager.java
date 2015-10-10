package com.proxerme.app.manager;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.proxerme.library.entity.LoginUser;

/**
 * Todo: Describe Class
 *
 * @author Ruben Gees
 */
public class UserManager {
    private static UserManager ourInstance = new UserManager();
    private LoginUser user;

    private UserManager() {
        user = StorageManager.getUser();
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
        StorageManager.removeUser();
    }

    public void changeUser(@NonNull LoginUser user) {
        StorageManager.setUser(user);
    }
}
