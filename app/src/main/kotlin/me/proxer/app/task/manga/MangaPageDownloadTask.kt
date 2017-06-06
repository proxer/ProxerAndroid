package me.proxer.app.task.manga

import com.rubengees.ktask.base.LeafTask
import me.proxer.app.application.MainApplication
import me.proxer.app.task.manga.MangaPageDownloadTask.MangaPageDownloadTaskInput
import me.proxer.library.util.ProxerUrls
import okhttp3.Call
import okhttp3.Request
import okio.Okio
import java.io.File
import java.io.IOException
import kotlin.concurrent.read

/**
 * @author Ruben Gees
 */
class MangaPageDownloadTask(private val filesDir: File) : LeafTask<MangaPageDownloadTaskInput, File>() {

    override val isWorking: Boolean
        get() = call != null

    private var call: Call? = null

    override fun execute(input: MangaPageDownloadTaskInput) {
        start {
            MangaLockHolder.cleanLock.read {
                val lockKey = Triple(input.entryId, input.chapterId, input.name)

                synchronized(MangaLockHolder.pageLocks.getOrPut(lockKey, { Any() }), {
                    val url = ProxerUrls.mangaPageImage(input.server, input.entryId, input.chapterId, input.name)
                    val file = File("$filesDir/manga/${input.entryId}/${input.chapterId}/${url.hashCode()}.0")

                    try {
                        if (file.exists()) {
                            finishSuccessful(file)
                        } else {
                            file.parentFile.mkdirs()
                            file.createNewFile()

                            if (isCancelled) {
                                throw(RuntimeException())
                            }

                            call = MainApplication.client.newCall(Request.Builder()
                                    .header("Accept-Encoding", "identity")
                                    .url(url)
                                    .build())

                            call?.execute()?.let {
                                if (it.isSuccessful) {
                                    it.body()?.use { theBody ->
                                        Okio.buffer(Okio.sink(file)).use { fileBuffer ->
                                            fileBuffer.writeAll(theBody.source())
                                        }
                                    } ?: throw IOException()

                                    internalCancel()
                                    finishSuccessful(file)
                                } else {
                                    throw IOException()
                                }
                            }
                        }
                    } catch (error: Throwable) {
                        file.delete()

                        internalCancel()
                        finishWithError(error)
                    }
                })
            }
        }
    }

    override fun cancel() {
        super.cancel()

        internalCancel()
    }

    private fun internalCancel() {
        call?.cancel()
        call = null
    }

    class MangaPageDownloadTaskInput(val server: String, val entryId: String, val chapterId: String, val name: String)
}