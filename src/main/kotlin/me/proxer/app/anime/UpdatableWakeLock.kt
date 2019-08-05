package me.proxer.app.anime

import android.os.PowerManager

/**
 * @author Ruben Gees
 */
class UpdatableWakeLock(
    powerManager: PowerManager,
    levelAndFlags: Int,
    tag: String
) {

    private val wakeLock = powerManager.newWakeLock(levelAndFlags, tag).apply {
        setReferenceCounted(false)
    }

    fun acquire(timeout: Long) {
        wakeLock.release()
        wakeLock.acquire(timeout)
    }

    fun release() {
        wakeLock.release()
    }
}
