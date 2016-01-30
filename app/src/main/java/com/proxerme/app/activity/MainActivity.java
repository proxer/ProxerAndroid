package com.proxerme.app.activity;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.proxerme.app.R;
import com.proxerme.app.customtabs.CustomTabActivityHelper;
import com.proxerme.app.customtabs.WebviewFallback;
import com.proxerme.app.manager.UserManager;
import com.proxerme.library.connection.ProxerConnection;

/**
 * This Activity does some work, all Activities have in common and all Activities should
 * inherit from this one.
 *
 * @author Ruben Gees
 */
public abstract class MainActivity extends AppCompatActivity {

    private CustomTabActivityHelper customTabActivityHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        customTabActivityHelper = new CustomTabActivityHelper();
    }

    @Override
    protected void onStart() {
        super.onStart();

        customTabActivityHelper.bindCustomTabsService(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        customTabActivityHelper.unbindCustomTabsService(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing()) {
            ProxerConnection.cleanup();
            UserManager.getInstance().destroy();
        }
    }

    public void setLikelyUrl(@NonNull String url) {
        customTabActivityHelper.mayLaunchUrl(Uri.parse(url), null, null);
    }

    public void showPage(@NonNull String url) {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder(customTabActivityHelper
                .getSession()).setToolbarColor(ContextCompat.getColor(this, R.color.primary))
                .enableUrlBarHiding().setShowTitle(true).build();

        CustomTabActivityHelper.openCustomTab(
                this, customTabsIntent, Uri.parse(url), new WebviewFallback());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isDestroyedCompat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed();
    }
}
