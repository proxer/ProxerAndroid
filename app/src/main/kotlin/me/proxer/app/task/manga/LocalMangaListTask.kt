package me.proxer.app.task.manga

import me.proxer.app.entity.MangaChapterInfo

/**
 * @author Ruben Gees
 */
class LocalMangaListTask : com.rubengees.ktask.util.WorkerTask<Unit, List<MangaChapterInfo>>() {
    override fun work(input: Unit): List<me.proxer.app.entity.MangaChapterInfo> {
        return emptyList()
//        return mangaDb.getAll()
    }
}