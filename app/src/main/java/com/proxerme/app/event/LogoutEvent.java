package com.proxerme.app.event;

import android.support.annotation.Nullable;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class LogoutEvent {

    private Integer errorCode;

    public LogoutEvent(@Nullable Integer errorMessage) {
        this.errorCode = errorMessage;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogoutEvent that = (LogoutEvent) o;

        return errorCode != null ? errorCode.equals(that.errorCode) : that.errorCode == null;

    }

    @Override
    public int hashCode() {
        return errorCode != null ? errorCode.hashCode() : 0;
    }
}
