package com.rubengees.proxerme.connection;

import android.support.annotation.NonNull;

import com.afollestad.bridge.BridgeException;

import static com.rubengees.proxerme.connection.ProxerException.*;
import static com.rubengees.proxerme.connection.ProxerException.ErrorCodes.IO;
import static com.rubengees.proxerme.connection.ProxerException.ErrorCodes.NETWORK;
import static com.rubengees.proxerme.connection.ProxerException.ErrorCodes.PROXER;
import static com.rubengees.proxerme.connection.ProxerException.ErrorCodes.TIMEOUT;
import static com.rubengees.proxerme.connection.ProxerException.ErrorCodes.UNKNOWN;
import static com.rubengees.proxerme.connection.ProxerException.ErrorCodes.UNPARSEABLE;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class ErrorHandler {

    @NonNull
    public static String getMessageForErrorCode(@ErrorCode int code) {
        switch (code) {
            case NETWORK:
                return "There was a problem with the network. Please check if you are connected";
            case TIMEOUT:
                return "The server didn't answer in time. There may be problems with your network";
            case UNPARSEABLE:
                return "The server sent corrupt data. Please try again";
            case IO:
                return "There was a problem with your storage. Please try again";
            case UNKNOWN:
                return "An unknown error occurred. Please try again";
            default:
                return "An unknown error occurred. Please try again";
        }
    }

    @NonNull
    public static ProxerException handleException(@NonNull BridgeException bridgeException){
        ProxerException exception;

        switch (bridgeException.reason()) {
            case BridgeException.REASON_REQUEST_TIMEOUT: {
                exception = new ProxerException(TIMEOUT,
                        getMessageForErrorCode(TIMEOUT));
                break;
            }
            case BridgeException.REASON_RESPONSE_UNSUCCESSFUL: {
                exception = new ProxerException(NETWORK,
                        getMessageForErrorCode(NETWORK));
                break;
            }
            case BridgeException.REASON_RESPONSE_UNPARSEABLE: {
                exception = new ProxerException(UNPARSEABLE,
                        getMessageForErrorCode(UNPARSEABLE));
                break;
            }
            case BridgeException.REASON_RESPONSE_IOERROR: {
                exception = new ProxerException(IO,
                        getMessageForErrorCode(IO));
                break;
            }
            case BridgeException.REASON_RESPONSE_VALIDATOR_FALSE:
                exception = new ProxerException(UNKNOWN,
                        getMessageForErrorCode(UNKNOWN));
                break;
            case BridgeException.REASON_RESPONSE_VALIDATOR_ERROR:
                exception = new ProxerException(PROXER, bridgeException.getMessage());
                break;
            default:
                exception = new ProxerException(UNKNOWN,
                        getMessageForErrorCode(UNKNOWN));
                break;
        }

        return exception;
    }

}
