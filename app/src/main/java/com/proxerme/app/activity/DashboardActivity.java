package com.proxerme.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.BadgeStyle;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.proxerme.app.R;
import com.proxerme.app.dialog.LoginDialog;
import com.proxerme.app.fragment.NewsFragment;
import com.proxerme.app.fragment.SettingsFragment;
import com.proxerme.app.interfaces.OnActivityListener;
import com.proxerme.app.manager.PreferenceManager;
import com.proxerme.app.manager.StorageManager;
import com.proxerme.app.manager.UserManager;
import com.proxerme.app.util.SnackbarManager;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.entity.LoginUser;
import com.rubengees.introduction.IntroductionActivity;
import com.rubengees.introduction.IntroductionBuilder;
import com.rubengees.introduction.IntroductionConfiguration;
import com.rubengees.introduction.entity.Option;
import com.rubengees.introduction.entity.Slide;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.proxerme.app.manager.NewsManager.NEWS_ON_PAGE;
import static com.proxerme.app.manager.NewsManager.OFFSET_NOT_CALCULABLE;
import static com.proxerme.app.manager.NewsManager.getInstance;

/**
 * This Activity provides the navigation to all different sections through the Drawer.
 *
 * @author Ruben Gees
 */
public class DashboardActivity extends MainActivity {

    public static final int DRAWER_ID_NEWS = 0;
    public static final int DRAWER_ID_INFO = 10;
    public static final int DRAWER_ID_DONATE = 11;
    public static final int DRAWER_ID_SETTINGS = 12;
    public static final String EXTRA_DRAWER_ITEM = "extra_drawer_item";
    private static final int DRAWER_ID_DEFAULT = DRAWER_ID_NEWS;
    private static final int HEADER_ID_GUEST = 100;
    private static final int HEADER_ID_USER = 101;
    private static final int HEADER_ID_LOGIN = 111;
    private static final int HEADER_ID_CHANGE = 112;
    private static final String STATE_CURRENT_DRAWER_ITEM_ID = "current_drawer_item_id";

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    private AccountHeader header;
    private Drawer drawer;

    private int currentDrawerItemId = -1;

    private OnActivityListener onActivityListener;

    private Drawer.OnDrawerListener onDrawerListener = new Drawer.OnDrawerListener() {
        @Override
        public void onDrawerOpened(View view) {
            SnackbarManager.dismiss();
        }

        @Override
        public void onDrawerClosed(View view) {
            if (onActivityListener != null) {
                onActivityListener.showErrorIfNecessary();
            }
        }

        @Override
        public void onDrawerSlide(View view, float v) {

        }
    };

    private Drawer.OnDrawerItemClickListener onDrawerItemClickListener =
            new Drawer.OnDrawerItemClickListener() {
                @Override
                public boolean onItemClick(View view, int position, IDrawerItem iDrawerItem) {
                    int id = iDrawerItem.getIdentifier();

                    if (id != currentDrawerItemId) {
                        if (iDrawerItem.isSelectable()) {
                            currentDrawerItemId = id;
                        }

                        return handleOnDrawerItemClick(id);
                    }

                    return true;
                }
            };
    private LoginDialog.LoginDialogCallback loginDialogCallback =
            new LoginDialog.LoginDialogCallback() {
                @Override
                public void onLogin(LoginUser user) {
                    UserManager.getInstance().changeUser(user);
                    refreshHeader();
                }
            };
    private AccountHeader.OnAccountHeaderListener onAccountHeaderClickListener =
            new AccountHeader.OnAccountHeaderListener() {
                @Override
                public boolean onProfileChanged(View view, IProfile profile, boolean current) {
                    switch (profile.getIdentifier()) {
                        case HEADER_ID_GUEST:
                            showLoginDialog();
                            break;
                        case HEADER_ID_USER:
                            //Don't do anything for now
                            break;
                        case HEADER_ID_LOGIN:
                            showLoginDialog();
                            break;
                        case HEADER_ID_CHANGE:
                            showLoginDialog();
                            break;
                    }

                    return false;
                }
            };

    public static Intent getSectionIntent(@NonNull Context context, int drawerItemId) {
        Intent intent = new Intent(context, DashboardActivity.class);

        intent.putExtra(EXTRA_DRAWER_ITEM, drawerItemId);
        return intent;
    }

    private void showLoginDialog() {
        LoginDialog dialog = LoginDialog.newInstance();

        dialog.setCallback(loginDialogCallback);
        dialog.show(getSupportFragmentManager(), "dialog_login");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        if (savedInstanceState != null) {
            currentDrawerItemId = savedInstanceState.getInt(STATE_CURRENT_DRAWER_ITEM_ID);

            try {
                onActivityListener = (OnActivityListener) getSupportFragmentManager()
                        .findFragmentById(R.id.activity_main_content_container);
            } catch (ClassCastException e) {
                onActivityListener = null;
            }

            LoginDialog dialog = (LoginDialog) getSupportFragmentManager()
                    .findFragmentByTag("dialog_login");

            if (dialog != null) {
                dialog.setCallback(loginDialogCallback);
            }
        }

        ButterKnife.bind(this);
        initViews();
        initHeader(savedInstanceState);
        initDrawer(savedInstanceState);

        int drawerItemToLoad = getIntent().getIntExtra(EXTRA_DRAWER_ITEM, -1);

        if (drawerItemToLoad == -1) {
            if (savedInstanceState == null) {
                if (StorageManager.isFirstStart()) {
                    initIntroduction();
                } else {
                    drawer.setSelection(DRAWER_ID_DEFAULT);
                }
            }
        } else if (savedInstanceState == null) {
            drawer.setSelection(drawerItemToLoad);
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
                            PreferenceManager.setNotificationsEnabled(DashboardActivity.this,
                                    option.isActivated());
                    }
                }
            }

            StorageManager.setFirstStartOccurred();
            drawer.setSelection(DRAWER_ID_DEFAULT);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        drawer.saveInstanceState(outState);
        header.saveInstanceState(outState);
        outState.putInt(STATE_CURRENT_DRAWER_ITEM_ID, currentDrawerItemId);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        } else if (onActivityListener == null) {
            handleBackPressed();
        } else {
            if (!onActivityListener.onBackPressed()) {
                handleBackPressed();
            }
        }
    }

    public void setBadge(int drawerItemId, @Nullable String text) {
        if (text == null) {
            drawer.updateBadge(drawerItemId, null);
        } else {
            drawer.updateBadge(drawerItemId, new StringHolder(text));
        }
    }

    private void handleBackPressed() {
        if (currentDrawerItemId == DRAWER_ID_DEFAULT) {
            super.onBackPressed();
        } else {
            drawer.setSelection(DRAWER_ID_DEFAULT);
        }
    }

    private void initViews() {
        setSupportActionBar(toolbar);
    }

    private void initHeader(Bundle savedInstanceState) {
        header = new AccountHeaderBuilder().withHeaderBackground(R.color.accent)
                .withActivity(this).withSavedInstance(savedInstanceState)
                .withProfiles(generateProfiles())
                .withOnAccountHeaderListener(onAccountHeaderClickListener).build();
    }

    private ArrayList<IProfile> generateProfiles() {
        LoginUser user = UserManager.getInstance().getUser();
        ArrayList<IProfile> result = new ArrayList<>();

        if (user == null) {
            result.add(new ProfileDrawerItem().withName(getString(R.string.drawer_profile_guest))
                    .withIcon(R.mipmap.ic_launcher)
                    .withIdentifier(HEADER_ID_GUEST));
            result.add(new ProfileSettingDrawerItem().withName(getString(R.string.drawer_header_login))
                    .withIcon(GoogleMaterial.Icon.gmd_person_add).withIdentifier(HEADER_ID_LOGIN));
        } else {
            result.add(new ProfileDrawerItem().withName(user.getUsername())
                    .withIdentifier(HEADER_ID_USER));
            result.add(new ProfileSettingDrawerItem().withName(getString(R.string.drawer_header_change))
                    .withIcon(GoogleMaterial.Icon.gmd_group).withIdentifier(HEADER_ID_CHANGE));
        }

        return result;
    }

    private void initDrawer(@Nullable Bundle savedInstanceState) {
        drawer = new DrawerBuilder(this)
                .withAccountHeader(header)
                .withDrawerItems(generateDrawerItems())
                .withStickyDrawerItems(generateStickyDrawerItems())
                .withOnDrawerListener(onDrawerListener)
                .withOnDrawerItemClickListener(onDrawerItemClickListener)
                .withShowDrawerOnFirstLaunch(true).withToolbar(toolbar)
                .withActionBarDrawerToggleAnimated(true).withHasStableIds(true)
                .withSavedInstance(savedInstanceState).build();

        initBadges();
    }

    @NonNull
    private ArrayList<IDrawerItem> generateDrawerItems() {
        ArrayList<IDrawerItem> result = new ArrayList<>(1);

        result.add(new PrimaryDrawerItem().withName(R.string.drawer_item_news)
                .withIcon(GoogleMaterial.Icon.gmd_dashboard)
                .withSelectedTextColorRes(R.color.primary).withSelectedIconColorRes(R.color.primary)
                .withIconTintingEnabled(true).withBadgeStyle(new BadgeStyle()
                        .withColorRes(R.color.primary).withTextColorRes(android.R.color.white))
                .withIdentifier(DRAWER_ID_NEWS));

        return result;
    }

    @NonNull
    private ArrayList<IDrawerItem> generateStickyDrawerItems() {
        ArrayList<IDrawerItem> result = new ArrayList<>(3);

        result.add(new PrimaryDrawerItem().withName(R.string.drawer_item_info)
                .withIcon(GoogleMaterial.Icon.gmd_info).withSelectedTextColorRes(R.color.primary)
                .withSelectedIconColorRes(R.color.primary).withIconTintingEnabled(true)
                .withIdentifier(DRAWER_ID_INFO));

        result.add(new PrimaryDrawerItem().withName(R.string.drawer_item_donate)
                .withIcon(GoogleMaterial.Icon.gmd_attach_money)
                .withSelectedTextColorRes(R.color.primary).withSelectedIconColorRes(R.color.primary)
                .withIconTintingEnabled(true).withSelectable(false)
                .withIdentifier(DRAWER_ID_DONATE));

        result.add(new PrimaryDrawerItem().withName(R.string.drawer_item_settings)
                .withIcon(GoogleMaterial.Icon.gmd_settings)
                .withSelectedTextColorRes(R.color.primary).withSelectedIconColorRes(R.color.primary)
                .withIconTintingEnabled(true).withIdentifier(DRAWER_ID_SETTINGS));

        return result;
    }

    private void initBadges() {
        int newNews = getInstance(this).getNewNews();

        if (newNews > 0 || newNews == OFFSET_NOT_CALCULABLE) {
            setBadge(DRAWER_ID_NEWS, newNews == OFFSET_NOT_CALCULABLE ? (NEWS_ON_PAGE + "+") :
                    (String.valueOf(newNews)));
        }
    }

    private void initIntroduction() {
        new IntroductionBuilder(this).withSlides(generateSlides())
                .withOnSlideListener(new IntroductionConfiguration.OnSlideListener() {
                    @Override
                    protected void onSlideInit(int position, @NonNull TextView title,
                                               @NonNull ImageView image,
                                               @NonNull TextView description) {
                        switch (position) {
                            case 0:
                                Glide.with(image.getContext())
                                        .load(R.drawable.ic_introduction_proxer).into(image);
                                break;
                            case 1:
                                Glide.with(image.getContext())
                                        .load(R.drawable.ic_introduction_notifications).into(image);
                                break;
                        }
                    }
                }).introduceMyself();
    }

    @NonNull
    private List<Slide> generateSlides() {
        List<Slide> slides = new ArrayList<>(2);

        slides.add(new Slide().withTitle(R.string.introduction_welcome_title)
                .withColorResource(R.color.primary)
                .withDescription(R.string.introduction_welcome_description));
        slides.add(new Slide().withTitle(R.string.introduction_notifications_title)
                .withColorResource(R.color.accent)
                .withOption(new Option(getString(R.string.introduction_notifications_description),
                        false)));

        return slides;
    }

    public void setFragment(@NonNull Fragment fragment, @NonNull String title) {
        setTitle(title);

        setFragment(fragment);
    }

    public void setFragment(@NonNull Fragment fragment) {
        if (fragment instanceof OnActivityListener) {
            onActivityListener = (OnActivityListener) fragment;
        } else {
            onActivityListener = null;
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.activity_main_content_container,
                fragment).commit();
    }

    private void refreshHeader() {
        header.setProfiles(generateProfiles());
    }

    private boolean handleOnDrawerItemClick(int id) {
        switch (id) {
            case DRAWER_ID_NEWS:
                setFragment(NewsFragment.newInstance(), getString(R.string.drawer_item_news));
                return false;
            case DRAWER_ID_INFO:
                setFragment(new LibsBuilder().withAboutVersionShownName(true)
                                .withAboutDescription(getString(R.string.about_description))
                                .withAboutIconShown(true).withAutoDetect(false)
                                .withAboutAppName(getString(R.string.app_name))
                                .withLibraries("glide", "systembartint", "jodatimeandroid",
                                        "bridge", "hawk", "butterknife", "materialdialogs")
                                .withFields(R.string.class.getFields()).fragment(),
                        getString(R.string.drawer_item_info));
                return false;
            case DRAWER_ID_DONATE:
                showPage(UrlHolder.getDonateUrl());
                return true;
            case DRAWER_ID_SETTINGS:
                setFragment(SettingsFragment.newInstance(),
                        getString(R.string.drawer_item_settings));
                return false;
            default:
                return true;
        }
    }
}
