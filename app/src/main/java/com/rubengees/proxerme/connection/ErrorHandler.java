/*
 *   Copyright 2015 Ruben Gees
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.rubengees.proxerme.connection;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.afollestad.bridge.BridgeException;
import com.rubengees.proxerme.R;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.rubengees.proxerme.connection.ErrorHandler.ErrorCodes.IO;
import static com.rubengees.proxerme.connection.ErrorHandler.ErrorCodes.NETWORK;
import static com.rubengees.proxerme.connection.ErrorHandler.ErrorCodes.PROXER;
import static com.rubengees.proxerme.connection.ErrorHandler.ErrorCodes.TIMEOUT;
import static com.rubengees.proxerme.connection.ErrorHandler.ErrorCodes.UNKNOWN;
import static com.rubengees.proxerme.connection.ErrorHandler.ErrorCodes.UNPARSEABLE;

/**
 * A Helper class, which converts an {@link Exception} to a integer, represented through the
 * Annotation {@link com.rubengees.proxerme.connection.ErrorHandler.ErrorCode}. It also has
 * a Method to convert a ErrorCode into a human readable {@link String}.
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

    @NonNull
    public static ProxerException handleException(@NonNull BridgeException bridgeException) {
        ProxerException exception;

        switch (bridgeException.reason()) {
            case BridgeException.REASON_REQUEST_TIMEOUT: {
                exception = new ProxerException(TIMEOUT);
                break;
            }
            case BridgeException.REASON_RESPONSE_UNSUCCESSFUL: {
                exception = new ProxerException(NETWORK);
                break;
            }
            case BridgeException.REASON_RESPONSE_UNPARSEABLE: {
                exception = new ProxerException(UNPARSEABLE);
                break;
            }
            case BridgeException.REASON_RESPONSE_IOERROR: {
                exception = new ProxerException(IO);
                break;
            }
            case BridgeException.REASON_RESPONSE_VALIDATOR_FALSE:
                exception = new ProxerException(UNKNOWN);
                break;
            case BridgeException.REASON_RESPONSE_VALIDATOR_ERROR:
                exception = new ProxerException(PROXER, bridgeException.getMessage());
                break;
            default:
                exception = new ProxerException(UNKNOWN);
                break;
        }

        return exception;
    }

    @IntDef({PROXER, NETWORK, UNPARSEABLE, IO,
            TIMEOUT, UNKNOWN})
    @Retention(value = RetentionPolicy.SOURCE)
    @Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
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
