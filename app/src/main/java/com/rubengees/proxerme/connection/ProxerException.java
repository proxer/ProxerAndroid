package com.rubengees.proxerme.connection;

import static com.rubengees.proxerme.connection.ErrorHandler.ErrorCode;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class ProxerException extends Exception {

    private int errorCode;

    public ProxerException(@ErrorCode int errorCode) {
        this.errorCode = errorCode;
    }

    public ProxerException(int errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    }

    @ErrorCode
    public int getErrorCode() {
        return errorCode;
    }
}
