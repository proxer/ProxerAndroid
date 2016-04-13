package com.proxerme.app.util;

import android.content.Context;
import android.support.annotation.NonNull;

import com.proxerme.app.R;
import com.proxerme.library.connection.ProxerException;

/**
 * A helper class, turning error codes into human-readable Strings.
 *
 * @author Ruben Gees
 */
public class ErrorHandler {

    @NonNull
    public static String getMessageForErrorCode(@NonNull Context context,
                                                ProxerException exception) {
        switch (exception.getErrorCode()) {
            case ProxerException.ERROR_PROXER:
                return exception.getMessage();
            case ProxerException.ERROR_NETWORK:
                return context.getString(R.string.error_network);
            case ProxerException.ERROR_TIMEOUT:
                return context.getString(R.string.error_timeout);
            case ProxerException.ERROR_UNPARSEABLE:
                return context.getString(R.string.error_unparseable);
            case ProxerException.ERROR_IO:
                return context.getString(R.string.error_io);
            case ProxerException.ERROR_UNKNOWN:
                return context.getString(R.string.error_unknown);
            default:
                return context.getString(R.string.error_unknown);
        }
    }

}
