package me.proxer.app.task.manga

import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.application.MainApplication
import me.proxer.app.application.MainApplication.Companion.globalContext
import java.io.File
import kotlin.concurrent.write

/**
 * @author Ruben Gees
 */
class MangaRemovalTask : WorkerTask<Unit, Unit>() {

    override fun work(input: Unit) {
        MainApplication.mangaDb.clear()

        MangaLockHolder.localLock.write {
            File("${globalContext.filesDir}/manga").deleteRecursively()
        }
    }
}
