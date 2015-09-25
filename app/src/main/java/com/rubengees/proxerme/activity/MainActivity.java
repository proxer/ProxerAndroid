package com.rubengees.proxerme.activity;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.afollestad.bridge.Bridge;
import com.rubengees.proxerme.R;
import com.rubengees.proxerme.connection.ProxerConnection;
import com.rubengees.proxerme.customtabs.CustomTabActivityHelper;
import com.rubengees.proxerme.customtabs.WebviewFallback;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
@SuppressLint("Registered")
public class MainActivity extends AppCompatActivity {

    private CustomTabActivityHelper customTabActivityHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ProxerConnection.initBridge();
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

    public void setLikelyUrl(String url) {
        customTabActivityHelper.mayLaunchUrl(Uri.parse(url), null, null);
    }

    public void showPage(String url) {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder(customTabActivityHelper
                .getSession()).setToolbarColor(ContextCompat.getColor(this, R.color.primary))
                .build();

        CustomTabActivityHelper.openCustomTab(
                this, customTabsIntent, Uri.parse(url), new WebviewFallback());
    }
}
