package com.proxerme.app.util;

import android.content.Context;
import android.support.annotation.NonNull;

import com.proxerme.app.R;
import com.proxerme.library.connection.ErrorHandler.ErrorCode;

import static com.proxerme.library.connection.ErrorHandler.ErrorCodes.IO;
import static com.proxerme.library.connection.ErrorHandler.ErrorCodes.NETWORK;
import static com.proxerme.library.connection.ErrorHandler.ErrorCodes.TIMEOUT;
import static com.proxerme.library.connection.ErrorHandler.ErrorCodes.UNKNOWN;
import static com.proxerme.library.connection.ErrorHandler.ErrorCodes.UNPARSEABLE;

/**
 * Todo: Describe Class
 *
 * @author Ruben Gees
 */
public class ErrorHandler {

    @NonNull
    public static String getMessageForErrorCode(@NonNull Context context, @ErrorCode int code) {
        switch (code) {
            case NETWORK:
                return context.getString(R.string.error_network);
            case TIMEOUT:
                return context.getString(R.string.error_timeout);
            case UNPARSEABLE:
                return context.getString(R.string.error_unparseable);
            case IO:
                return context.getString(R.string.error_io);
            case UNKNOWN:
                return context.getString(R.string.error_unknown);
            default:
                return context.getString(R.string.error_unknown);
        }
    }

}
