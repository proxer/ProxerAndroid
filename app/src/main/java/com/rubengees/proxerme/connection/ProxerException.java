package com.rubengees.proxerme.connection;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class ProxerException extends Exception {

    private int errorCode;

    public ProxerException(@ErrorCode int errorCode, @NonNull String detailMessage) {
        super(detailMessage);

        this.errorCode = errorCode;
    }

    @ErrorCode
    public int getErrorCode() {
        return errorCode;
    }

    @IntDef({ErrorCodes.PROXER, ErrorCodes.NETWORK, ErrorCodes.UNPARSEABLE, ErrorCodes.IO,
            ErrorCodes.TIMEOUT, ErrorCodes.UNKNOWN})
    @Retention(value = RetentionPolicy.SOURCE)
    public @interface ErrorCode {
    }

    public class ErrorCodes {
        public static final int PROXER = 0;
        public static final int NETWORK = 1;
        public static final int UNPARSEABLE = 2;
        public static final int IO = 3;
        public static final int TIMEOUT = 4;
        public static final int UNKNOWN = 5;
    }
}
