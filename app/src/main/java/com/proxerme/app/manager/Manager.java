package com.proxerme.app.manager;

import org.greenrobot.eventbus.EventBus;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public abstract class Manager {

    public Manager() {
        EventBus.getDefault().register(this);
    }

    public void destroy() {
        EventBus.getDefault().unregister(this);
    }
}
