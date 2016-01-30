package com.proxerme.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;

import com.proxerme.app.R;
import com.proxerme.app.dialog.LoginDialog;
import com.proxerme.app.dialog.LogoutDialog;
import com.proxerme.app.fragment.ConferencesFragment;
import com.proxerme.app.fragment.NewsFragment;
import com.proxerme.app.fragment.SettingsFragment;
import com.proxerme.app.interfaces.OnActivityListener;
import com.proxerme.app.manager.PreferenceManager;
import com.proxerme.app.manager.StorageManager;
import com.proxerme.app.manager.UserManager;
import com.proxerme.app.util.IntroductionHelper;
import com.proxerme.app.util.MaterialDrawerHelper;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.event.success.LoginEvent;
import com.proxerme.library.event.success.LogoutEvent;
import com.rubengees.introduction.IntroductionActivity;
import com.rubengees.introduction.IntroductionBuilder;
import com.rubengees.introduction.entity.Option;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

import static com.proxerme.app.util.MaterialDrawerHelper.DRAWER_ID_DEFAULT;
import static com.proxerme.app.util.MaterialDrawerHelper.DRAWER_ID_DONATE;
import static com.proxerme.app.util.MaterialDrawerHelper.DRAWER_ID_MESSAGES;
import static com.proxerme.app.util.MaterialDrawerHelper.DRAWER_ID_NEWS;
import static com.proxerme.app.util.MaterialDrawerHelper.DRAWER_ID_NONE;
import static com.proxerme.app.util.MaterialDrawerHelper.DRAWER_ID_SETTINGS;
import static com.proxerme.app.util.MaterialDrawerHelper.HEADER_ID_CHANGE;
import static com.proxerme.app.util.MaterialDrawerHelper.HEADER_ID_GUEST;
import static com.proxerme.app.util.MaterialDrawerHelper.HEADER_ID_LOGIN;
import static com.proxerme.app.util.MaterialDrawerHelper.HEADER_ID_LOGOUT;
import static com.proxerme.app.util.MaterialDrawerHelper.HEADER_ID_USER;
import static com.proxerme.app.util.MaterialDrawerHelper.MaterialDrawerCallback;

/**
 * This Activity provides the navigation to all different sections through the Drawer.
 *
 * @author Ruben Gees
 */
public class DashboardActivity extends MainActivity {

    private static final String EXTRA_DRAWER_ITEM = "extra_drawer_item";
    private static final String EXTRA_ADDITIONAL_INFO = "extra_additional_info";
    private static final String STATE_TITLE = "dashboard_title";

    @Bind(R.id.root)
    ViewGroup root;
    @Bind(R.id.toolbar_container)
    AppBarLayout toolbarContainer;
    @Bind(R.id.toolbar)
    Toolbar toolbar;

    private MaterialDrawerHelper drawerHelper;
    private OnActivityListener onActivityListener;

    private String title;

    private Snackbar snackbar;

    private MaterialDrawerCallback drawerCallback = new MaterialDrawerCallback() {
        @Override
        public boolean onItemClick(int identifier) {
            return handleOnDrawerItemClick(identifier);
        }

        @Override
        public boolean onAccountClick(int identifier) {
            return handleOnHeaderAccountClick(identifier);
        }
    };

    public static Intent getSectionIntent(@NonNull Context context,
                                          @MaterialDrawerHelper.DrawerItemId int drawerItemId,
                                          @Nullable String additionalInfo) {
        Intent intent = new Intent(context, DashboardActivity.class);

        intent.putExtra(EXTRA_DRAWER_ITEM, drawerItemId);
        if (additionalInfo != null) {
            intent.putExtra(EXTRA_ADDITIONAL_INFO, additionalInfo);
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        drawerHelper = new MaterialDrawerHelper(this, drawerCallback);

        ButterKnife.bind(this);
        initViews();
        drawerHelper.build(toolbar, savedInstanceState);

        displayFirstPage(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();

        EventBus.getDefault().registerSticky(this);

        UserManager userManager = UserManager.getInstance();

        if (!userManager.isLoggedIn() && userManager.getUser() != null) {
            userManager.login(userManager.getUser());
        }
    }

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);

        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction() != null && !intent.getAction().equals(Intent.ACTION_MAIN)) {
            setIntent(intent);

            displayFirstPage(null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IntroductionBuilder.INTRODUCTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                for (Option option : data.<Option>getParcelableArrayListExtra(IntroductionActivity.
                        OPTION_RESULT)) {
                    switch (option.getPosition()) {
                        case 1:
                            PreferenceManager.setNewsNotificationsEnabled(DashboardActivity.this,
                                    option.isActivated());
                            PreferenceManager.setMessagesNotificationsEnabled(DashboardActivity.this,
                                    option.isActivated());

                            break;
                    }
                }
            }

            StorageManager.setFirstStartOccurred();
            drawerHelper.select(DRAWER_ID_DEFAULT);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(STATE_TITLE, title);
        drawerHelper.saveInstanceState(outState);
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

    @Override
    public void onBackPressed() {
        if (drawerHelper.isDrawerOpen()) {
            drawerHelper.handleBackPressed();
        } else if (onActivityListener == null) {
            drawerHelper.handleBackPressed();
        } else {
            if (!onActivityListener.onBackPressed()) {
                if (!drawerHelper.handleBackPressed()) {
                    super.onBackPressed();
                }
            }
        }
    }

    public void onEventMainThread(LoginEvent event) {
        if (!isDestroyedCompat()) {
            drawerHelper.refreshHeader();
        }
    }

    public void onEventMainThread(LogoutEvent event) {
        if (!isDestroyedCompat()) {
            drawerHelper.refreshHeader();
        }
    }

    private void displayFirstPage(@Nullable Bundle savedInstanceState) {
        int drawerItemToLoad = getItemToLoad(getIntent());

        if (drawerItemToLoad == DRAWER_ID_NONE) {
            if (savedInstanceState == null) {
                if (StorageManager.isFirstStart()) {
                    IntroductionHelper.build(this);
                } else {
                    drawerHelper.select(DRAWER_ID_DEFAULT);
                }
            }
        } else if (savedInstanceState == null) {
            drawerHelper.select(drawerItemToLoad);
        }
    }

    @MaterialDrawerHelper.DrawerItemId
    private int getItemToLoad(@NonNull Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            String url = intent.getDataString();

            if (url.contains("news")) {
                return MaterialDrawerHelper.DRAWER_ID_NEWS;
            } else if (url.contains("messages")) {
                return MaterialDrawerHelper.DRAWER_ID_MESSAGES;
            } else {
                return MaterialDrawerHelper.DRAWER_ID_NONE;
            }
        } else {
            //noinspection ResourceType
            return intent.getIntExtra(EXTRA_DRAWER_ITEM, MaterialDrawerHelper.DRAWER_ID_NONE);
        }
    }

    public void setBadge(@MaterialDrawerHelper.DrawerItemId int drawerItemId,
                         @Nullable String text) {
        drawerHelper.setBadge(drawerItemId, text);
    }

    private void initViews() {
        setSupportActionBar(toolbar);
    }

    public void setFragment(@NonNull Fragment fragment, @NonNull String title) {
        this.title = title;
        setTitle(title);

        setFragment(fragment);
    }

    public void setFragment(@NonNull final Fragment fragment) {
        if (fragment instanceof OnActivityListener) {
            onActivityListener = (OnActivityListener) fragment;
        } else {
            onActivityListener = null;
        }

        toolbarContainer.setExpanded(true);
        clearMessage();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.activity_main_content_container, fragment).commit();
            }
        });
    }

    private boolean handleOnHeaderAccountClick(@MaterialDrawerHelper.HeaderItemId int id) {
        switch (id) {
            case HEADER_ID_GUEST:
                showLoginDialog();
                return false;
            case HEADER_ID_USER:
                //Don't do anything for now
                return false;
            case HEADER_ID_LOGIN:
                showLoginDialog();
                return false;
            case HEADER_ID_CHANGE:
                showLoginDialog();
                return false;
            case HEADER_ID_LOGOUT:
                showLogoutDialog();
                return false;
            default:
                return false;
        }
    }

    public void showLoginDialog() {
        LoginDialog.newInstance().show(getSupportFragmentManager(), "dialog_login");
    }

    public void showLogoutDialog() {
        LogoutDialog.newInstance().show(getSupportFragmentManager(), "dialog_logout");
    }

    public void showMessage(@NonNull String message, @Nullable String action,
                            @Nullable View.OnClickListener listener) {
        snackbar = Snackbar.make(root, message, Snackbar.LENGTH_INDEFINITE)
                .setAction(action, listener);

        snackbar.show();
    }

    public void clearMessage() {
        if (snackbar != null) {
            snackbar.dismiss();

            snackbar = null;
        }
    }

    private boolean handleOnDrawerItemClick(@MaterialDrawerHelper.DrawerItemId int id) {
        switch (id) {
            case DRAWER_ID_NEWS:
                setFragment(NewsFragment.newInstance(), getString(R.string.drawer_item_news));
                return false;
            case DRAWER_ID_MESSAGES:
                setFragment(ConferencesFragment.newInstance(),
                        getString(R.string.drawer_item_messages));
                return false;
            case DRAWER_ID_DONATE:
                showPage(UrlHolder.getDonateUrl());
                return true;
            case DRAWER_ID_SETTINGS:
                setFragment(SettingsFragment.newInstance(),
                        getString(R.string.drawer_item_settings));
                return false;
            case MaterialDrawerHelper.DRAWER_ID_NONE:
                throw new RuntimeException("Unknown drawer id");
            default:
                return true;
        }
    }
}
