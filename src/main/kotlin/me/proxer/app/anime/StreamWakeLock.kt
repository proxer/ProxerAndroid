package me.proxer.app.anime

import android.net.wifi.WifiManager
import android.os.PowerManager

/**
 * @author Ruben Gees
 */
class StreamWakeLock(
    powerManager: PowerManager,
    wifiManager: WifiManager,
    tag: String
) {

    private val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag).apply {
        setReferenceCounted(false)
    }

    private val wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, tag).apply {
        setReferenceCounted(false)
    }

    fun acquire(timeout: Long) {
        wakeLock.release()
        wifiLock.release()

        wakeLock.acquire(timeout)
        wifiLock.acquire()
    }

    fun release() {
        wakeLock.release()
        wifiLock.release()
    }
}
