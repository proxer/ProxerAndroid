package com.proxerme.app.util;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public abstract class EventBusBuffer {

    private ConcurrentLinkedQueue<Object> queue;

    public EventBusBuffer() {
        queue = new ConcurrentLinkedQueue<>();
    }

    public void startBuffering() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    public void stopAndProcess() {
        safeUnregister();

        while (!queue.isEmpty()) {
            EventBus.getDefault().post(queue.poll());
        }
    }

    public void stopAndPurge() {
        safeUnregister();

        queue.clear();
    }

    private void safeUnregister() {
        EventBus.getDefault().unregister(this);
    }

    protected void addToQueue(Object event) {
        queue.add(event);
    }

}
