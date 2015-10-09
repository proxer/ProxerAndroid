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

package com.rubengees.proxerme.activity;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.afollestad.bridge.Bridge;
import com.proxerme.library.connection.ProxerConnection;
import com.rubengees.proxerme.R;
import com.rubengees.proxerme.customtabs.CustomTabActivityHelper;
import com.rubengees.proxerme.customtabs.WebviewFallback;

/**
 * This Activity does some work, all Activities have in common and all Activities should
 * inherit from this one.
 *
 * @author Ruben Gees
 */
@SuppressLint("Registered")
public class MainActivity extends AppCompatActivity {

    private CustomTabActivityHelper customTabActivityHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ProxerConnection.init();
        customTabActivityHelper = new CustomTabActivityHelper();
    }

    @Override
    protected void onStart() {
        super.onStart();

        customTabActivityHelper.bindCustomTabsService(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        customTabActivityHelper.unbindCustomTabsService(this);
        Bridge.cleanup();
    }

    public void setLikelyUrl(@NonNull String url) {
        customTabActivityHelper.mayLaunchUrl(Uri.parse(url), null, null);
    }

    public void showPage(@NonNull String url) {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder(customTabActivityHelper
                .getSession()).setToolbarColor(ContextCompat.getColor(this, R.color.primary))
                .build();

        CustomTabActivityHelper.openCustomTab(
                this, customTabsIntent, Uri.parse(url), new WebviewFallback());
    }
}
