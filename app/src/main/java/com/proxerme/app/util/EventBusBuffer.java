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

    private static EventBusBuffer instance;
    private ConcurrentLinkedQueue<Object> queue;

    private EventBusBuffer() {
        this.queue = new ConcurrentLinkedQueue<>();
    }

    public static void startBuffering() {
        if (!EventBus.getDefault().isRegistered(getInstance())) {
            EventBus.getDefault().register(getInstance());
        }
    }

    public static void stopAndProcess() {
        safeUnregister();

        while (!getInstance().queue.isEmpty()) {
            EventBus.getDefault().post(getInstance().queue.poll());
        }
    }

    public static void stopAndPurge() {
        safeUnregister();

        getInstance().queue.clear();
    }

    private static EventBusBuffer getInstance() {
        if (instance == null) {
            instance = new EventBusBuffer();
        }

        return instance;
    }

    private static void safeUnregister() {
        EventBus.getDefault().unregister(getInstance());
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
