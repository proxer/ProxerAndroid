package me.proxer.app.task.manga

import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.application.MainApplication.Companion.mangaDb
import me.proxer.library.entitiy.info.EntryCore

/**
 * @author Ruben Gees
 */
class LocalMangaEntryTask : WorkerTask<String, EntryCore>() {
    override fun work(input: String) = mangaDb.findEntry(input) ?: throw RuntimeException("No entry found")
}