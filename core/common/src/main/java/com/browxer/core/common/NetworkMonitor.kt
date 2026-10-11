package com.browxer.core.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkMonitor {
    /** Wi-Fi gate for the Wi-Fi-only setting: unmetered Wi-Fi only. */
    fun isOnUnmeteredWifi(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? ConnectivityManager ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}
