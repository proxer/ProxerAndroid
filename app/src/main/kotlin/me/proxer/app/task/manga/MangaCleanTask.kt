package me.proxer.app.task.manga

import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.application.MainApplication.Companion.globalContext
import me.proxer.app.entity.manga.MangaInput
import java.io.File
import kotlin.concurrent.write

/**
 * @author Ruben Gees
 */
class MangaCleanTask : WorkerTask<MangaInput, MangaInput>() {

    private companion object {
        private const val MAX_CACHE_SIZE = 1024L * 1024L * 256L
    }

    override fun work(input: MangaInput): MangaInput {
        MangaLockHolder.cacheLock.write {
            val localDataInfos = arrayListOf<LocalDataInfo>()
            val cacheDir = globalContext.cacheDir
            var overallSize = 0L

            File("$cacheDir/manga").list()?.forEach { entryDirectoryName ->
                if (isCancelled) {
                    throw RuntimeException()
                }

                File("$cacheDir/manga/$entryDirectoryName").list()?.forEach { chapterDirectoryName ->
                    val chapterDirectory = File("${globalContext.cacheDir}/manga/$entryDirectoryName/$chapterDirectoryName")
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

            while (overallSize >= MAX_CACHE_SIZE) {
                localDataInfos.minBy { it.lastModification }?.let {
                    File("$cacheDir/manga/${it.entryId}/${it.chapterId}").deleteRecursively()

                    overallSize -= it.size
                }
            }
        }

        return input
    }

    private class LocalDataInfo(val entryId: String, val chapterId: String, val lastModification: Long, val size: Long)
}