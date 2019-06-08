@file:Suppress("DEPRECATION")

package me.proxer.app.util.compat

import android.net.ConnectivityManager
import android.os.Build

val ConnectivityManager.isConnected
    get() = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> activeNetwork != null
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> activeNetworkInfo != null
        else -> activeNetworkInfo?.isConnectedOrConnecting ?: false
    }
