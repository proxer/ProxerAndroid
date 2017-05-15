package me.proxer.app.task.manga

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * @author Ruben Gees
 */
internal object MangaLockHolder {

    internal val cleanLock = ReentrantReadWriteLock()
    internal val pageLocks = ConcurrentHashMap<Triple<String, String, String>, Any>()
}