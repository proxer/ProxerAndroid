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

import android.support.annotation.Nullable;

import static com.rubengees.proxerme.connection.ErrorHandler.ErrorCode;

/**
 * A subclass of {@link Exception}, representing all possible connection problems.
 *
 * @author Ruben Gees
 */
public class ProxerException extends Exception {

    @ErrorCode
    private int errorCode;

    public ProxerException(@ErrorCode int errorCode) {
        this.errorCode = errorCode;
    }

    public ProxerException(@ErrorCode int errorCode, @Nullable String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    }

    @ErrorCode
    public int getErrorCode() {
        return errorCode;
    }
}
