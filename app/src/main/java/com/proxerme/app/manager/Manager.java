package com.proxerme.app.manager;

import org.greenrobot.eventbus.EventBus;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public abstract class Manager {

    public Manager() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }
}
