package com.proxerme.app.activity;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;

import com.proxerme.app.R;
import com.proxerme.app.application.MainApplication;
import com.proxerme.app.customtabs.CustomTabActivityHelper;
import com.proxerme.app.customtabs.WebviewFallback;
import com.proxerme.app.interfaces.OnActivityListener;
import com.proxerme.app.util.EventBusBuffer;
import com.proxerme.app.util.Utils;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * This Activity does some work, all Activities have in common and all Activities should
 * inherit from this one.
 *
 * @author Ruben Gees
 */
public abstract class MainActivity extends AppCompatActivity {

    private static final String STATE_TITLE = "title";

    @Bind(R.id.activity_main_content_container)
    ViewGroup content;
    @Bind(R.id.toolbar_container)
    AppBarLayout toolbarContainer;
    @Bind(R.id.toolbar)
    Toolbar toolbar;

    private CustomTabActivityHelper customTabActivityHelper;

    private String title;
    private OnActivityListener onActivityListener;
    private Snackbar snackbar;

    private EventBusBuffer eventBusBuffer = new EventBusBuffer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        initViews();

        customTabActivityHelper = new CustomTabActivityHelper();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        eventBusBuffer.stopAndProcess();
    }

    @Override
    protected void onPause() {
        super.onPause();

        eventBusBuffer.startBuffering();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        eventBusBuffer.stopAndPurge();
    }

    @Override
    protected void onStart() {
        super.onStart();

        customTabActivityHelper.bindCustomTabsService(this);
    }

    @Override
    protected void onStop() {
        customTabActivityHelper.unbindCustomTabsService(this);

        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(STATE_TITLE, title);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null) {
            try {
                onActivityListener = (OnActivityListener) getSupportFragmentManager()
                        .findFragmentById(R.id.activity_main_content_container);
            } catch (ClassCastException e) {
                onActivityListener = null;
            }

            title = savedInstanceState.getString(STATE_TITLE);

            setTitle(title);
        }
    }

    protected boolean handleBackPressed() {
        return onActivityListener != null && onActivityListener.onBackPressed();
    }

    public void setFragment(@NonNull Fragment fragment, @NonNull String title) {
        this.title = title;

        setTitle(title);
        setFragment(fragment);
    }

    public void setFragment(@NonNull final Fragment fragment) {
        if (Utils.areActionsPossible(this)) {
            if (fragment instanceof OnActivityListener) {
                onActivityListener = (OnActivityListener) fragment;
            } else {
                onActivityListener = null;
            }

            toolbarContainer.setExpanded(true);
            clearMessage();

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.activity_main_content_container, fragment).commit();
        }
    }

    public void showMessage(@NonNull String message, @Nullable String action,
                            @Nullable View.OnClickListener listener, int duration) {
        snackbar = Snackbar.make(content, message, duration)
                .setActionTextColor(ContextCompat.getColorStateList(this, R.color.colorPrimaryLight))
                .setAction(action, listener);

        snackbar.show();
    }

    public void showMessage(@NonNull String message, @Nullable String action,
                            @Nullable View.OnClickListener listener) {
        showMessage(message, action, listener, Snackbar.LENGTH_INDEFINITE);
    }

    public void clearMessage() {
        if (snackbar != null) {
            snackbar.dismiss();

            snackbar = null;
        }
    }

    public void setLikelyUrl(@NonNull String url) {
        customTabActivityHelper.mayLaunchUrl(Uri.parse(url), null, null);
    }

    public void showPage(@NonNull String url) {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder(customTabActivityHelper
                .getSession()).setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setSecondaryToolbarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                .enableUrlBarHiding().setShowTitle(true).build();

        CustomTabActivityHelper.openCustomTab(
                this, customTabsIntent, Uri.parse(url), new WebviewFallback());
    }

    protected final MainApplication getMainApplication() {
        return (MainApplication) getApplication();
    }

    private void initViews() {
        setSupportActionBar(toolbar);
    }
}
