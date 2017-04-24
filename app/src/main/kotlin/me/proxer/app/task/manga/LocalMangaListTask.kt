package me.proxer.app.task.manga

import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.application.MainApplication.Companion.mangaDb
import me.proxer.app.util.extension.CompleteLocalMangaEntry

/**
 * @author Ruben Gees
 */
class LocalMangaListTask : WorkerTask<Unit, List<CompleteLocalMangaEntry>>() {
    override fun work(input: Unit): List<CompleteLocalMangaEntry> {
        return mangaDb.getAll()
    }
}