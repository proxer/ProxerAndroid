package com.proxerme.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.proxerme.app.R;
import com.proxerme.app.dialog.LoginDialog;
import com.proxerme.app.dialog.LogoutDialog;
import com.proxerme.app.fragment.ConferencesFragment;
import com.proxerme.app.fragment.NewsFragment;
import com.proxerme.app.fragment.SettingsFragment;
import com.proxerme.app.manager.BadgeManager;
import com.proxerme.app.manager.PreferenceManager;
import com.proxerme.app.manager.StorageManager;
import com.proxerme.app.manager.UserManager;
import com.proxerme.app.util.Utils;
import com.proxerme.app.util.helper.IntroductionHelper;
import com.proxerme.app.util.helper.MaterialDrawerHelper;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.event.success.LoginEvent;
import com.proxerme.library.event.success.LogoutEvent;
import com.rubengees.introduction.IntroductionActivity;
import com.rubengees.introduction.IntroductionBuilder;
import com.rubengees.introduction.entity.Option;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.proxerme.app.util.helper.MaterialDrawerHelper.DRAWER_ID_DONATE;
import static com.proxerme.app.util.helper.MaterialDrawerHelper.DRAWER_ID_MESSAGES;
import static com.proxerme.app.util.helper.MaterialDrawerHelper.DRAWER_ID_NEWS;
import static com.proxerme.app.util.helper.MaterialDrawerHelper.DRAWER_ID_NONE;
import static com.proxerme.app.util.helper.MaterialDrawerHelper.DRAWER_ID_SETTINGS;
import static com.proxerme.app.util.helper.MaterialDrawerHelper.HEADER_ID_CHANGE;
import static com.proxerme.app.util.helper.MaterialDrawerHelper.HEADER_ID_GUEST;
import static com.proxerme.app.util.helper.MaterialDrawerHelper.HEADER_ID_LOGIN;
import static com.proxerme.app.util.helper.MaterialDrawerHelper.HEADER_ID_LOGOUT;
import static com.proxerme.app.util.helper.MaterialDrawerHelper.HEADER_ID_USER;
import static com.proxerme.app.util.helper.MaterialDrawerHelper.MaterialDrawerCallback;

/**
 * This Activity provides the navigation to all different sections through the Drawer.
 *
 * @author Ruben Gees
 */
public class DashboardActivity extends MainActivity {

    private static final String EXTRA_DRAWER_ITEM = "extra_drawer_item";
    private static final String EXTRA_ADDITIONAL_INFO = "extra_additional_info";

    private MaterialDrawerHelper drawerHelper;
    private BadgeManager badgeManager;

    private MaterialDrawerCallback drawerCallback = new MaterialDrawerCallback() {
        @Override
        public boolean onItemClick(@MaterialDrawerHelper.DrawerItemId int identifier) {
            return handleOnDrawerItemClick(identifier);
        }

        @Override
        public boolean onAccountClick(@MaterialDrawerHelper.HeaderItemId int identifier) {
            return handleOnHeaderAccountClick(identifier);
        }
    };

    private BadgeManager.BadgeCallback badgeCallback = new BadgeManager.BadgeCallback() {
        @Override
        public void updateBadge(@MaterialDrawerHelper.DrawerItemId int id, @Nullable String count) {
            drawerHelper.setBadge(id, count);
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

        drawerHelper = new MaterialDrawerHelper(this, drawerCallback);
        drawerHelper.build(toolbar, savedInstanceState);

        badgeManager = new BadgeManager(badgeCallback);

        displayFirstPage(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
        badgeManager.startListenForEvents();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        badgeManager.stopListenForEvents();

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing()) {
            ProxerConnection.cleanup();
            UserManager.getInstance().destroy();
        }
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
            drawerHelper.select(DRAWER_ID_NEWS);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        drawerHelper.saveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (drawerHelper.isDrawerOpen()) {
            drawerHelper.handleBackPressed();
        } else if (!handleBackPressed()) {
            if (!drawerHelper.handleBackPressed()) {
                super.onBackPressed();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onLogin(LoginEvent event) {
        if (!Utils.isDestroyedCompat(this)) {
            drawerHelper.refreshHeader();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onLogout(LogoutEvent event) {
        if (!Utils.isDestroyedCompat(this)) {
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
                    drawerHelper.select(DRAWER_ID_NEWS);
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

    private void showLoginDialog() {
        if (Utils.areActionsPossible(this)) {
            LoginDialog.show(this);
        }
    }

    private void showLogoutDialog() {
        if (Utils.areActionsPossible(this)) {
            LogoutDialog.show(this);
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
