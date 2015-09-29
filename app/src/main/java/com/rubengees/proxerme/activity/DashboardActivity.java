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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.rubengees.introduction.IntroductionActivity;
import com.rubengees.introduction.IntroductionBuilder;
import com.rubengees.introduction.IntroductionConfiguration;
import com.rubengees.introduction.entity.Option;
import com.rubengees.introduction.entity.Slide;
import com.rubengees.proxerme.R;
import com.rubengees.proxerme.connection.UrlHolder;
import com.rubengees.proxerme.fragment.NewsFragment;
import com.rubengees.proxerme.fragment.SettingsFragment;
import com.rubengees.proxerme.interfaces.OnActivityListener;
import com.rubengees.proxerme.manager.PreferenceManager;
import com.rubengees.proxerme.util.SnackbarManager;

import java.util.ArrayList;
import java.util.List;

import static com.rubengees.proxerme.manager.NewsManager.NEWS_ON_PAGE;
import static com.rubengees.proxerme.manager.NewsManager.OFFSET_NOT_CALCULABLE;
import static com.rubengees.proxerme.manager.NewsManager.getInstance;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class DashboardActivity extends MainActivity {

    public static final int DRAWER_ID_NEWS = 0;
    public static final int DRAWER_ID_INFO = 10;
    public static final int DRAWER_ID_DONATE = 11;
    public static final int DRAWER_ID_SETTINGS = 12;

    public static final String EXTRA_DRAWER_ITEM = "extra_drawer_item";
    private static final String STATE_CURRENT_DRAWER_ITEM_ID = "current_drawer_item_id";

    private Toolbar toolbar;
    private FrameLayout contentContainer;

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

    public static Intent getSectionIntent(@NonNull Context context, int drawerItemId) {
        Intent intent = new Intent(context, DashboardActivity.class);

        intent.putExtra(EXTRA_DRAWER_ITEM, drawerItemId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        if (savedInstanceState != null) {
            currentDrawerItemId = savedInstanceState.getInt(STATE_CURRENT_DRAWER_ITEM_ID);
            onActivityListener = (OnActivityListener) getSupportFragmentManager()
                    .findFragmentById(R.id.activity_main_content_container);
        }

        findViews();
        initViews();
        initDrawer(savedInstanceState);

        int drawerItemToLoad = getIntent().getIntExtra(EXTRA_DRAWER_ITEM, -1);

        if (drawerItemToLoad == -1) {
            if (savedInstanceState == null) {
                if (PreferenceManager.isFirstStart(this)) {
                    initIntroduction();
                } else {
                    drawer.setSelection(DRAWER_ID_NEWS);
                }
            }
        } else {
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

            PreferenceManager.setFirstStartOccurred(this);
            drawer.setSelection(DRAWER_ID_NEWS);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        drawer.saveInstanceState(outState);
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
        if (currentDrawerItemId == DRAWER_ID_NEWS) {
            super.onBackPressed();
        } else {
            drawer.setSelection(DRAWER_ID_NEWS);
        }
    }

    private void findViews() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        contentContainer = (FrameLayout) findViewById(R.id.activity_main_content_container);
    }

    private void initViews() {
        setSupportActionBar(toolbar);
    }

    private void initDrawer(@Nullable Bundle savedInstanceState) {
        drawer = new DrawerBuilder(this).withDrawerItems(generateDrawerItems())
                .withStickyDrawerItems(generateStickyDrawerItems())
                .withOnDrawerListener(onDrawerListener)
                .withOnDrawerItemClickListener(onDrawerItemClickListener)
                .withShowDrawerOnFirstLaunch(true).withSelectedItem(DRAWER_ID_NEWS)
                .withActionBarDrawerToggleAnimated(true).withHasStableIds(true)
                .withSavedInstance(savedInstanceState).withToolbar(toolbar).build();

        int newNews = getInstance(this).getNewNews();

        if (newNews > 0 || newNews == OFFSET_NOT_CALCULABLE) {
            setBadge(DRAWER_ID_NEWS, newNews == OFFSET_NOT_CALCULABLE ? (NEWS_ON_PAGE + "+") :
                    (String.valueOf(newNews)));
        }
    }

    private ArrayList<IDrawerItem> generateDrawerItems() {
        ArrayList<IDrawerItem> result = new ArrayList<>(1);

        result.add(new PrimaryDrawerItem().withName(R.string.drawer_item_news)
                .withIcon(GoogleMaterial.Icon.gmd_dashboard)
                .withSelectedTextColorRes(R.color.primary).withSelectedIconColorRes(R.color.primary)
                .withIconTintingEnabled(true).withIdentifier(DRAWER_ID_NEWS));

        return result;
    }

    private ArrayList<IDrawerItem> generateStickyDrawerItems() {
        ArrayList<IDrawerItem> result = new ArrayList<>(1);

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

    private void initIntroduction() {
        new IntroductionBuilder(this).withSlides(generateSlides())
                .withOnSlideListener(new IntroductionConfiguration.OnSlideListener() {
                    @Override
                    protected void onSlideInit(Fragment context, int position, TextView title,
                                               ImageView image, TextView description) {
                        switch (position) {
                            case 0:
                                Glide.with(context).load(R.drawable.ic_introduction_proxer)
                                        .into(image);
                                break;
                            case 1:
                                Glide.with(context).load(R.drawable.ic_introduction_notifications)
                                        .into(image);
                                break;
                        }
                    }
                }).introduceMyself();
    }

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

    private boolean handleOnDrawerItemClick(int id) {
        switch (id) {
            case DRAWER_ID_NEWS:
                setFragment(NewsFragment.newInstance(), getString(R.string.drawer_item_news));
                return false;
            case DRAWER_ID_INFO:
                setFragment(new LibsBuilder().withAboutVersionShownName(true)
                        .withAboutDescription(getString(R.string.about_description)).withAboutIconShown(true)
                        .withAboutAppName(getString(R.string.app_name)).withAutoDetect(false)
                        .withLibraries("glide", "jodatimeandroid", "bridge").fragment(), getString(R.string.drawer_item_info));
                return false;
            case DRAWER_ID_DONATE:
                showPage(UrlHolder.getDonateUrl());
                return true;
            case DRAWER_ID_SETTINGS:
                setFragment(SettingsFragment.newInstance(), getString(R.string.drawer_item_settings));
                return false;
            default:
                return true;
        }
    }
}
