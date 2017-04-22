package me.proxer.app.task

import com.rubengees.ktask.base.BranchTask
import com.rubengees.ktask.base.Task
import com.rubengees.ktask.operation.CacheTask
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.application.MainApplication
import me.proxer.app.application.MainApplication.Companion.mangaDb
import me.proxer.app.entity.MangaChapterInfo
import me.proxer.app.entity.MangaInput
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.info.EntryCore
import me.proxer.library.entitiy.manga.Chapter

/**
 * @author Ruben Gees
 */
class MangaTask : BranchTask<MangaInput, MangaChapterInfo,
        Pair<ProxerCall<Chapter>, ProxerCall<EntryCore>>, MangaChapterInfo>() {

    override val innerTask = TaskBuilder.asyncProxerTask<Chapter>()
            .parallelWith(
                    TaskBuilder.asyncProxerTask<EntryCore>()
                            .cache(CacheTask.CacheStrategy.RESULT),
                    zipFunction = { chapter, info -> MangaChapterInfo(chapter, info.name, info.episodeAmount) },
                    awaitRightResultOnError = true
            ).build()

    init {
        initCallbacks()
    }

    override fun execute(input: MangaInput) {
        start {
            try {
                val offlineChapterInfo = mangaDb.find(input.id, input.episode, input.language)

                if (offlineChapterInfo != null) {
                    finishSuccessful(offlineChapterInfo)
                } else {
                    innerTask.forceExecute(MainApplication.api.manga()
                            .chapter(input.id, input.episode, input.language)
                            .build() to MainApplication.api.info()
                            .entryCore(input.id)
                            .build())
                }
            } catch (error: Throwable) {
                finishWithError(error)
            }
        }
    }

    override fun restoreCallbacks(from: Task<MangaInput, MangaChapterInfo>) {
        super.restoreCallbacks(from)

        initCallbacks()
    }

    private fun initCallbacks() {
        innerTask.onSuccess {
            finishSuccessful(it)
        }

        innerTask.onError {
            finishWithError(it)
        }
    }
}
