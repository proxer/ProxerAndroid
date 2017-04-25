package me.proxer.app.task.manga

import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.application.MainApplication.Companion.mangaDb
import me.proxer.app.entity.MangaInput
import me.proxer.library.entitiy.manga.Chapter

/**
 * @author Ruben Gees
 */
class LocalMangaChapterTask : WorkerTask<MangaInput, Chapter>() {
    override fun work(input: MangaInput) = mangaDb.findChapter(input.id, input.episode, input.language)
            ?: throw RuntimeException("No entry found")
}