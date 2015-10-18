package com.proxerme.app.util;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.mikepenz.community_material_typeface_library.CommunityMaterial;
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
import com.proxerme.app.manager.NewsManager;
import com.proxerme.app.manager.UserManager;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.entity.LoginUser;
import com.proxerme.library.util.ProxerInfo;

import java.util.ArrayList;

/**
 * Todo: Describe Class
 *
 * @author Ruben Gees
 */
public class MaterialDrawerHelper {

    public static final int DRAWER_ID_NONE = -1;

    //Normal items
    public static final int DRAWER_ID_NEWS = 0;
    public static final int DRAWER_ID_MESSAGES = 1;

    //Sticky items
    public static final int DRAWER_ID_INFO = 10;
    public static final int DRAWER_ID_DONATE = 11;
    public static final int DRAWER_ID_SETTINGS = 12;
    public static final int DRAWER_ID_DEFAULT = DRAWER_ID_NEWS;

    //Header items
    public static final int HEADER_ID_GUEST = 100;
    public static final int HEADER_ID_USER = 101;
    public static final int HEADER_ID_LOGIN = 111;
    public static final int HEADER_ID_CHANGE = 112;
    public static final int HEADER_ID_LOGOUT = 113;

    private static final String STATE_CURRENT_DRAWER_ITEM_ID = "current_drawer_item_id";

    private Activity context;

    private AccountHeader header;
    private Drawer drawer;

    private int currentDrawerItemId = -1;

    private MaterialDrawerCallback callback;
    private Drawer.OnDrawerListener onDrawerListener = new Drawer.OnDrawerListener() {
        @Override
        public void onDrawerOpened(View view) {
            SnackbarManager.dismiss();
        }

        @Override
        public void onDrawerClosed(View view) {
            if (callback != null) {
                callback.onDrawerClosed();
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

                        if (callback != null) {
                            return callback.onItemClick(id);
                        }
                    }

                    return true;
                }
            };

    private AccountHeader.OnAccountHeaderListener onAccountHeaderClickListener =
            new AccountHeader.OnAccountHeaderListener() {
                @Override
                public boolean onProfileChanged(View view, IProfile profile, boolean current) {
                    if (callback != null) {
                        return callback.onAccountClick(profile.getIdentifier());
                    }

                    return false;
                }
            };

    public MaterialDrawerHelper(@NonNull Activity context, @Nullable MaterialDrawerCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    public void build(@NonNull Toolbar toolbar, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            currentDrawerItemId = savedInstanceState.getInt(STATE_CURRENT_DRAWER_ITEM_ID);
        }

        initHeader(savedInstanceState);
        initDrawer(toolbar, savedInstanceState);
    }

    private void initHeader(Bundle savedInstanceState) {
        header = new AccountHeaderBuilder().withHeaderBackground(R.color.accent)
                .withActivity(context).withSavedInstance(savedInstanceState)
                .withProfiles(generateProfiles())
                .withOnAccountHeaderListener(onAccountHeaderClickListener).build();
    }

    private ArrayList<IProfile> generateProfiles() {
        LoginUser user = UserManager.getInstance().getUser();
        ArrayList<IProfile> result = new ArrayList<>();

        if (user == null) {
            result.add(new ProfileDrawerItem()
                    .withName(context.getString(R.string.drawer_profile_guest))
                    .withIcon(R.mipmap.ic_launcher)
                    .withIdentifier(HEADER_ID_GUEST));
            result.add(new ProfileSettingDrawerItem()
                    .withName(context.getString(R.string.drawer_header_login))
                    .withIcon(GoogleMaterial.Icon.gmd_person_add).withIdentifier(HEADER_ID_LOGIN));
        } else {
            ProfileDrawerItem profile = new ProfileDrawerItem().withName(user.getUsername())
                    .withIdentifier(HEADER_ID_USER);

            try {
                profile.withIcon(UrlHolder.getUserImage(user.getImageId()));
            } catch (RuntimeException e) {
                //ignore
            }

            result.add(profile);
            result.add(new ProfileSettingDrawerItem()
                    .withName(context.getString(R.string.drawer_header_change))
                    .withIcon(GoogleMaterial.Icon.gmd_group).withIdentifier(HEADER_ID_CHANGE));
            result.add(new ProfileSettingDrawerItem()
                    .withName(context.getString(R.string.drawer_header_logout))
                    .withIcon(GoogleMaterial.Icon.gmd_exit_to_app)
                    .withIdentifier(HEADER_ID_LOGOUT));
        }

        return result;
    }

    private void initDrawer(@NonNull Toolbar toolbar, @Nullable Bundle savedInstanceState) {
        drawer = new DrawerBuilder(context)
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
                .withIcon(CommunityMaterial.Icon.cmd_newspaper)
                .withSelectedTextColorRes(R.color.primary).withSelectedIconColorRes(R.color.primary)
                .withIconTintingEnabled(true).withBadgeStyle(new BadgeStyle()
                        .withColorRes(R.color.primary).withTextColorRes(android.R.color.white))
                .withIdentifier(DRAWER_ID_NEWS));

        result.add(new PrimaryDrawerItem().withName(R.string.drawer_item_messages)
                .withIcon(GoogleMaterial.Icon.gmd_chat).withSelectedTextColorRes(R.color.primary)
                .withSelectedIconColorRes(R.color.primary).withIconTintingEnabled(true)
                .withBadgeStyle(new BadgeStyle().withColorRes(R.color.primary)
                        .withTextColorRes(android.R.color.white)).withIdentifier(DRAWER_ID_MESSAGES));

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

    public void saveInstanceState(Bundle outState) {
        outState.putInt(STATE_CURRENT_DRAWER_ITEM_ID, currentDrawerItemId);
        header.saveInstanceState(outState);
        drawer.saveInstanceState(outState);
    }

    public boolean isDrawerOpen() {
        return drawer.isDrawerOpen();
    }

    public void select(int id) {
        drawer.setSelection(id);
    }

    public boolean handleBackPressed() {
        if (isDrawerOpen()) {
            drawer.closeDrawer();

            return true;
        } else if (currentDrawerItemId != DRAWER_ID_DEFAULT) {
            select(DRAWER_ID_DEFAULT);

            return true;
        } else {
            return false;
        }
    }

    public void setBadge(int drawerItemId, @Nullable String text) {
        if (text == null) {
            drawer.updateBadge(drawerItemId, null);
        } else {
            drawer.updateBadge(drawerItemId, new StringHolder(text));
        }
    }

    private void initBadges() {
        int newNews = NewsManager.getInstance(context).getNewNews();

        if (newNews > 0 || newNews == PagingHelper.OFFSET_NOT_CALCULABLE) {
            setBadge(DRAWER_ID_NEWS, newNews == PagingHelper.OFFSET_NOT_CALCULABLE ?
                    (ProxerInfo.NEWS_ON_PAGE + "+") : (String.valueOf(newNews)));
        }
    }

    public void refreshHeader() {
        header.setProfiles(generateProfiles());
    }

    public static abstract class MaterialDrawerCallback {
        public boolean onItemClick(int identifier) {
            return false;
        }

        public boolean onAccountClick(int identifier) {
            return false;
        }

        public void onDrawerOpened() {

        }

        public void onDrawerClosed() {

        }
    }
}
