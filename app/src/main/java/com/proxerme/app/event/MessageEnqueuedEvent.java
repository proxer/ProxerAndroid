package com.proxerme.app.event;

import android.support.annotation.NonNull;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public class MessageEnqueuedEvent {

    private String conferenceId;

    public MessageEnqueuedEvent(@NonNull String conferenceId) {
        this.conferenceId = conferenceId;
    }

    @NonNull
    public String getConferenceId() {
        return conferenceId;
    }
}
