package me.proxer.app.application

import me.proxer.app.EventBusIndex
import org.greenrobot.eventbus.EventBus

/**
 * @author Ruben Gees
 */
object KaptStub {

    fun initLibs() {
        EventBus.builder().addIndex(EventBusIndex())
                .logNoSubscriberMessages(false)
                .sendNoSubscriberEvent(false)
                .installDefaultEventBus()
    }
}