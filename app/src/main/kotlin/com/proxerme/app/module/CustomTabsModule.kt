package com.proxerme.app.module

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import com.proxerme.app.R
import customtabs.CustomTabActivityHelper
import customtabs.CustomTabsHelper
import customtabs.WebviewFallback
import okhttp3.HttpUrl

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
interface CustomTabsModule {

    val customTabActivityHelper: CustomTabActivityHelper

    fun setLikelyUrl(url: HttpUrl) {
        customTabActivityHelper.mayLaunchUrl(Uri.parse(url.toString()), Bundle(), listOf())
    }

    fun showPage(url: HttpUrl) {
        val self = this as Activity

        val customTabsIntent = CustomTabsIntent.Builder(customTabActivityHelper.session)
                .setToolbarColor(ContextCompat.getColor(self, R.color.colorPrimary))
                .setSecondaryToolbarColor(ContextCompat.getColor(self,
                        R.color.colorPrimaryDark))
                .enableUrlBarHiding()
                .setShowTitle(true)
                .build()

        CustomTabsHelper.addKeepAliveExtra(self, customTabsIntent.intent)
        CustomTabActivityHelper.openCustomTab(
                self, customTabsIntent, Uri.parse(url.toString()), WebviewFallback())
    }

}