package com.proxerme.app.util.helper;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.mikepenz.community_material_typeface_library.CommunityMaterial;
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
import com.proxerme.app.application.MainApplication;
import com.proxerme.app.manager.UserManager;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.entity.LoginUser;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

/**
 * A helper class for managing the MaterialDrawer.
 *
 * @author Ruben Gees
 */
public class MaterialDrawerHelper {

    public static final int DRAWER_ID_NONE = -1;

    //Normal items
    public static final int DRAWER_ID_NEWS = 0;
    public static final int DRAWER_ID_MESSAGES = 1;

    //Sticky items
    public static final int DRAWER_ID_DONATE = 10;
    public static final int DRAWER_ID_SETTINGS = 11;

    //Header items
    public static final int HEADER_ID_GUEST = 100;
    public static final int HEADER_ID_USER = 101;
    public static final int HEADER_ID_LOGIN = 111;
    public static final int HEADER_ID_CHANGE = 112;
    public static final int HEADER_ID_LOGOUT = 113;

    private static final String STATE_CURRENT_DRAWER_ITEM_ID = "drawer_current_drawer_item_id";

    private AccountHeader header;
    private Drawer drawer;

    private UserManager userManager;

    private int currentDrawerItemId = -1;

    private MaterialDrawerCallback callback;

    private Drawer.OnDrawerItemClickListener onDrawerItemClickListener =
            new Drawer.OnDrawerItemClickListener() {
                @Override
                public boolean onItemClick(View view, int position, IDrawerItem iDrawerItem) {
                    int id = (int) iDrawerItem.getIdentifier();

                    if (id != currentDrawerItemId) {
                        if (iDrawerItem.isSelectable()) {
                            currentDrawerItemId = id;
                        }

                        if (callback != null) {
                            //noinspection WrongConstant
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
                    //noinspection WrongConstant
                    return callback != null &&
                            callback.onAccountClick((int) profile.getIdentifier());
                }
            };

    public MaterialDrawerHelper(@NonNull Activity context,
                                @Nullable MaterialDrawerCallback callback) {
        this.callback = callback;

        this.userManager = ((MainApplication) context.getApplication()).getUserManager();
    }

    public void build(@NonNull Activity context, @NonNull Toolbar toolbar,
                      @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            currentDrawerItemId = savedInstanceState.getInt(STATE_CURRENT_DRAWER_ITEM_ID);
        }

        initHeader(context, savedInstanceState);
        initDrawer(context, toolbar, savedInstanceState);
    }

    private void initHeader(@NonNull Activity context, Bundle savedInstanceState) {
        header = new AccountHeaderBuilder()
                .withHeaderBackground(R.color.colorAccent)
                .withActivity(context)
                .withSavedInstance(savedInstanceState)
                .withProfiles(generateProfiles(context))
                .withOnAccountHeaderListener(onAccountHeaderClickListener)
                .build();
    }

    private ArrayList<IProfile> generateProfiles(@NonNull Activity context) {
        LoginUser user = userManager.getUser();
        ArrayList<IProfile> result = new ArrayList<>();

        if (user == null) {
            result.add(new ProfileDrawerItem()
                    .withName(context.getString(R.string.drawer_profile_guest))
                    .withIcon(R.mipmap.ic_launcher)
                    .withIdentifier(HEADER_ID_GUEST));
            result.add(new ProfileSettingDrawerItem()
                    .withName(context.getString(R.string.drawer_header_login))
                    .withIconTinted(true)
                    .withIcon(CommunityMaterial.Icon.cmd_account_key)
                    .withIdentifier(HEADER_ID_LOGIN));
        } else {
            ProfileDrawerItem profile = new ProfileDrawerItem().withName(user.getUsername())
                    .withIdentifier(HEADER_ID_USER);

            profile.withIcon(UrlHolder.getUserImageUrl(user.getImageId()));

            result.add(profile);
            result.add(new ProfileSettingDrawerItem()
                    .withName(context.getString(R.string.drawer_header_change))
                    .withIcon(CommunityMaterial.Icon.cmd_account_switch)
                    .withIconTinted(true)
                    .withIdentifier(HEADER_ID_CHANGE));
            result.add(new ProfileSettingDrawerItem()
                    .withName(context.getString(R.string.drawer_header_logout))
                    .withIcon(CommunityMaterial.Icon.cmd_account_remove)
                    .withIconTinted(true)
                    .withIdentifier(HEADER_ID_LOGOUT));
        }

        return result;
    }

    private void initDrawer(@NonNull Activity context, @NonNull Toolbar toolbar,
                            @Nullable Bundle savedInstanceState) {
        drawer = new DrawerBuilder(context)
                .withAccountHeader(header)
                .withDrawerItems(generateDrawerItems())
                .withStickyDrawerItems(generateStickyDrawerItems())
                .withOnDrawerItemClickListener(onDrawerItemClickListener)
                .withShowDrawerOnFirstLaunch(true).withToolbar(toolbar)
                .withActionBarDrawerToggleAnimated(true)
                .withTranslucentStatusBar(true)
                .withSavedInstance(savedInstanceState).build();
    }

    @NonNull
    private ArrayList<IDrawerItem> generateDrawerItems() {
        ArrayList<IDrawerItem> result = new ArrayList<>(2);

        result.add(new PrimaryDrawerItem().withName(R.string.drawer_item_news)
                .withIcon(CommunityMaterial.Icon.cmd_newspaper)
                .withSelectedTextColorRes(R.color.colorPrimary)
                .withSelectedIconColorRes(R.color.colorPrimary)
                .withBadgeStyle(new BadgeStyle()
                        .withColorRes(R.color.colorPrimary)
                        .withTextColorRes(android.R.color.white))
                .withIdentifier(DRAWER_ID_NEWS));

        result.add(new PrimaryDrawerItem().withName(R.string.drawer_item_messages)
                .withIcon(CommunityMaterial.Icon.cmd_message_text)
                .withSelectedTextColorRes(R.color.colorPrimary)
                .withSelectedIconColorRes(R.color.colorPrimary)
                .withBadgeStyle(new BadgeStyle()
                        .withColorRes(R.color.colorPrimary)
                        .withTextColorRes(android.R.color.white))
                .withIdentifier(DRAWER_ID_MESSAGES));

        return result;
    }

    @NonNull
    private ArrayList<IDrawerItem> generateStickyDrawerItems() {
        ArrayList<IDrawerItem> result = new ArrayList<>(2);

        result.add(new PrimaryDrawerItem().withName(R.string.drawer_item_donate)
                .withIcon(CommunityMaterial.Icon.cmd_gift)
                .withSelectedTextColorRes(R.color.colorPrimary)
                .withSelectedIconColorRes(R.color.colorPrimary)
                .withSelectable(false)
                .withIdentifier(DRAWER_ID_DONATE));

        result.add(new PrimaryDrawerItem().withName(R.string.drawer_item_settings)
                .withIcon(CommunityMaterial.Icon.cmd_settings)
                .withSelectedTextColorRes(R.color.colorPrimary)
                .withSelectedIconColorRes(R.color.colorPrimary)
                .withIdentifier(DRAWER_ID_SETTINGS));

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

    public void select(@DrawerItemId int id) {
        drawer.setSelection(id);
    }

    public boolean handleBackPressed() {
        if (isDrawerOpen()) {
            drawer.closeDrawer();

            return true;
        } else if (currentDrawerItemId != DRAWER_ID_NEWS) {
            select(DRAWER_ID_NEWS);

            return true;
        } else {
            return false;
        }
    }

    public void setBadge(@DrawerItemId int drawerItemId, @Nullable String text) {
        if (text == null) {
            drawer.updateBadge(drawerItemId, null);
        } else {
            drawer.updateBadge(drawerItemId, new StringHolder(text));
        }
    }

    public void refreshHeader(@NonNull Activity context) {
        header.setProfiles(generateProfiles(context));
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DRAWER_ID_NONE, DRAWER_ID_NEWS, DRAWER_ID_MESSAGES, DRAWER_ID_DONATE,
            DRAWER_ID_SETTINGS})
    public @interface DrawerItemId {
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({HEADER_ID_GUEST, HEADER_ID_USER, HEADER_ID_LOGIN, HEADER_ID_LOGOUT,
            HEADER_ID_CHANGE})
    public @interface HeaderItemId {
    }

    public static abstract class MaterialDrawerCallback {
        public boolean onItemClick(@DrawerItemId int identifier) {
            return false;
        }

        public boolean onAccountClick(@HeaderItemId int identifier) {
            return false;
        }
    }
}
