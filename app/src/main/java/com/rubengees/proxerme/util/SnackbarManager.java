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

package com.rubengees.proxerme.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.View;

/**
 * A manager for the current displayed {@link Snackbar} instance.
 *
 * @author Ruben Gees
 */
public class SnackbarManager {

    private static Snackbar current;
    private static boolean reshowing = false;

    public static void show(@NonNull final Snackbar snackbar, @Nullable String actionTitle,
                            final @Nullable SnackbarCallback callback) {
        dismiss();
        reshowing = true;
        current = snackbar;

        if (actionTitle != null && callback != null) {
            snackbar.setAction(actionTitle, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    current = null;

                    callback.onClick(v);
                }
            });
        }

        snackbar.getView().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {

            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                if (!reshowing) {
                    current = null;
                }

                if (callback != null) {
                    callback.onDismiss(v);
                }
            }
        });

        snackbar.show();
    }

    public static void dismiss() {
        reshowing = false;

        if (current != null) {
            current.dismiss();
        }
    }

    public static boolean hasSnackbar() {
        return current != null;
    }

    public static void update(@NonNull String text) {
        current.setText(text);
    }

    public static boolean isShowing() {
        return current != null && current.isShown();
    }

    public static abstract class SnackbarCallback {
        public void onDismiss(View v) {

        }

        public void onClick(View v) {

        }
    }
}
