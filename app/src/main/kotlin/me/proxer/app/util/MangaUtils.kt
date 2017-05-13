package me.proxer.app.util

import me.proxer.app.application.MainApplication
import me.proxer.app.application.MainApplication.Companion.mangaDb
import me.proxer.library.util.ProxerUrls
import okhttp3.Request
import okio.Okio
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * @author Ruben Gees
 */
object MangaUtils {

    private const val MAX_CACHE_SIZE = 1024L * 1024L * 512L

    private val cleanLock = ReentrantReadWriteLock()
    private val pageLocks = ConcurrentHashMap<Triple<String, String, String>, Any>()

    @Throws(IOException::class)
    fun downloadPage(filesDir: File, server: String, entryId: String, chapterId: String, name: String): File {
        cleanLock.read {
            val lockKey = Triple(entryId, chapterId, name)

            return synchronized(pageLocks.getOrPut(lockKey, { Any() }), {
                val url = ProxerUrls.mangaPageImage(server, entryId, chapterId, name)
                val file = File("$filesDir/manga/$entryId/$chapterId/${url.toString().hashCode()}.0")

                try {
                    if (file.exists()) {
                        return file
                    } else {
                        file.parentFile.mkdirs()
                        file.createNewFile()
                    }

                    MainApplication.client.newCall(Request.Builder().url(url).build()).execute().let {
                        if (!it.isSuccessful) {
                            throw IOException()
                        } else {
                            it.body()
                        }
                    }.use { body ->
                        Okio.buffer(Okio.sink(file)).use { fileBuffer ->
                            fileBuffer.writeAll(body.source())
                        }
                    }

                    file
                } catch (error: Throwable) {
                    file.delete()

                    throw error
                } finally {
                    pageLocks.remove(lockKey)
                }
            })
        }
    }

    @Throws(IOException::class)
    fun deletePages(filesDir: File, entryId: String, chapterId: String) {
        cleanLock.write {
            File("$filesDir/manga/$entryId/$chapterId").deleteRecursively()
        }
    }

    @Throws(IOException::class)
    fun deleteAllChapters(filesDir: File) {
        cleanLock.write {
            File("$filesDir/manga").deleteRecursively()
        }
    }

    @Throws(IOException::class)
    fun clean(filesDir: File) {
        cleanLock.write {
            val localDataInfos = arrayListOf<LocalDataInfo>()
            var overallSize = 0L

            File("$filesDir/manga").list()?.forEach { entryDirectoryName ->
                if (!mangaDb.containsChapter(entryDirectoryName)) {
                    File("$filesDir/manga/$entryDirectoryName").list()?.forEach { chapterDirectoryName ->
                        val chapterDirectory = File("$filesDir/manga/$entryDirectoryName/$chapterDirectoryName")
                        var size = 0L

                        chapterDirectory.walkTopDown().forEach {
                            if (it.isFile) {
                                size += it.length()
                                overallSize += size
                            }
                        }

                        localDataInfos += LocalDataInfo(entryDirectoryName, chapterDirectoryName,
                                chapterDirectory.lastModified(), size)
                    }
                }
            }

            while (overallSize >= MAX_CACHE_SIZE) {
                localDataInfos.minBy { it.lastModification }?.let {
                    File("$filesDir/manga/${it.entryId}/${it.chapterId}").deleteRecursively()

                    overallSize -= it.size
                }
            }
        }
    }

    private class LocalDataInfo(val entryId: String, val chapterId: String, val lastModification: Long, val size: Long)
}