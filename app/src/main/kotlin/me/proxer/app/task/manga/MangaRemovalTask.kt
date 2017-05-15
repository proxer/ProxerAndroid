package me.proxer.app.task.manga

import com.rubengees.ktask.util.WorkerTask
import java.io.File
import kotlin.concurrent.write

/**
 * @author Ruben Gees
 */
class MangaRemovalTask(private val filesDir: File) : WorkerTask<Unit, Unit>() {

    override fun work(input: Unit) {
        MangaLockHolder.cleanLock.write {
            File("$filesDir/manga").deleteRecursively()
        }
    }
}
