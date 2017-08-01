package me.proxer.app.manga

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * @author Ruben Gees
 */
object MangaLocks {

    internal val cacheLock = ReentrantReadWriteLock()
    internal val localLock = ReentrantReadWriteLock()
    internal val pageConcurrencyLock = Semaphore(3, false)
    internal val pageLocks = ConcurrentHashMap<Triple<String, String, String>, Unit>()
}
