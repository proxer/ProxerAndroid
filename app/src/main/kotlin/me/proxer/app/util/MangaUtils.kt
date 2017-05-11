package me.proxer.app.util

import me.proxer.app.application.MainApplication
import me.proxer.library.util.ProxerUrls
import okhttp3.Request
import okio.Okio
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Ruben Gees
 */
object MangaUtils {

    private val locks = ConcurrentHashMap<Triple<String, String, String>, Any>()

    @Throws(IOException::class)
    fun downloadPage(filesDir: File, server: String, entryId: String, chapterId: String, name: String): File {
        val lockKey = Triple(entryId, chapterId, name)

        return synchronized(locks.getOrPut(lockKey, { Any() }), {
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
                locks.remove(lockKey)
            }
        })
    }

    @Throws(IOException::class)
    fun deletePages(filesDir: File, entryId: String, chapterId: String) {
        File("$filesDir/manga/$entryId/$chapterId").deleteRecursively()
    }

    @Throws(IOException::class)
    fun deleteAllChapters(filesDir: File) {
        File("$filesDir/manga").deleteRecursively()
    }
}