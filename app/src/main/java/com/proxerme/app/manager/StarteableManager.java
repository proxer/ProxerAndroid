package com.proxerme.app.manager;

import org.greenrobot.eventbus.EventBus;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public abstract class StarteableManager {

    public void startListeningForEvents() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    public void stopListeningForEvents() {
        EventBus.getDefault().unregister(this);
    }

}
