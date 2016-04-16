package com.proxerme.app.util;

import com.proxerme.app.event.MessageEnqueuedEvent;
import com.proxerme.library.event.IEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public class EventBusBuffer {

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

    @Subscribe
    public void onProxerEvent(IEvent event) {
        queue.add(event);
    }

    @Subscribe
    public void onMessageEnqueuedEvent(MessageEnqueuedEvent event) {
        queue.add(event);
    }

}
