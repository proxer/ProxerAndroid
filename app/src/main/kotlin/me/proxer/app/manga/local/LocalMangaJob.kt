package me.proxer.app.manga.local

import android.content.Context
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.evernote.android.job.JobRequest.NetworkType
import com.evernote.android.job.util.support.PersistableBundleCompat
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.MainApplication.Companion.mangaDao
import me.proxer.app.manga.MangaPageSingle
import me.proxer.app.manga.MangaPageSingle.Input
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.decodedName
import me.proxer.app.util.extension.toLocalChapter
import me.proxer.app.util.extension.toLocalEntryCore
import me.proxer.app.util.extension.toLocalPage
import me.proxer.library.api.ProxerException
import me.proxer.library.api.ProxerException.ServerErrorType
import me.proxer.library.enums.Language
import me.proxer.library.util.ProxerUtils

/**
 * @author Ruben Gees
 */
class LocalMangaJob : Job() {

    companion object {
        const val TAG = "local_manga_job"

        private const val ENTRY_ID_EXTRA = "entry_id"
        private const val EPISODE_EXTRA = "episode"
        private const val LANGUAGE_EXTRA = "language"

        private val lock = Any()
        private var lastTime = 0L
        private var ongoing = 0

        fun schedule(context: Context, entryId: String, episode: Int, language: Language) {
            val isUnmeteredRequired = PreferenceHelper.isUnmeteredNetworkRequiredForMangaDownload(context)
            val extras = PersistableBundleCompat().apply {
                putString(ENTRY_ID_EXTRA, entryId)
                putInt(EPISODE_EXTRA, episode)
                putString(LANGUAGE_EXTRA, ProxerUtils.getApiEnumName(language))
            }

            JobRequest.Builder(constructTag(entryId, episode, language))
                    .setExtras(extras)
                    .setRequiredNetworkType(if (isUnmeteredRequired) NetworkType.UNMETERED else NetworkType.CONNECTED)
                    .setBackoffCriteria(15000, JobRequest.BackoffPolicy.LINEAR)
                    .setExecutionWindow(1L, 100L)
                    .setRequirementsEnforced(true)
                    .setUpdateCurrent(true)
                    .setPersisted(true)
                    .build()
                    .schedule()
        }

        fun cancel(entryId: String, episode: Int, language: Language) {
            JobManager.instance().cancelAllForTag(constructTag(entryId, episode, language))
        }

        fun cancelAll() {
            JobManager.instance().allJobRequests.filter { it.tag.startsWith(TAG) }
                    .map { it.tag }
                    .plus(JobManager.instance().allJobs.filter { it is LocalMangaJob }.map {
                        (it as LocalMangaJob).let { constructTag(it.entryId, it.episode, it.language) }
                    }).forEach { JobManager.instance().cancelAllForTag(it) }
        }

        fun isScheduledOrRunning(entryId: String, episode: Int, language: Language): Boolean {
            val isScheduled = JobManager.instance().allJobRequests.find {
                it.tag == constructTag(entryId, episode, language)
            } != null

            val isRunning = JobManager.instance().allJobs.find {
                it is LocalMangaJob && it.isJobFor(entryId, episode, language) && !it.isCanceled && !it.isFinished
            } != null

            return isScheduled || isRunning
        }

        fun countScheduledJobs() = JobManager.instance().allJobRequests.filter { it.tag.startsWith(TAG) }.size
        fun countRunningJobs() = JobManager.instance().allJobs.filter {
            it is LocalMangaJob && !it.isCanceled && !it.isFinished
        }.size

        private fun constructTag(entryId: String, episode: Int, language: Language): String {
            return "${TAG}_${entryId}_${episode}_${ProxerUtils.getApiEnumName(language)}"
        }
    }

    val entryId: String
        get() = params.extras.getString(ENTRY_ID_EXTRA, "-1") ?: throw IllegalArgumentException("No extras passed")

    val episode: Int
        get() = params.extras.getInt(EPISODE_EXTRA, -1)

    val language: Language
        get() = ProxerUtils.toApiEnum(Language::class.java, params.extras.getString(LANGUAGE_EXTRA, "en"))
                ?: throw IllegalArgumentException("No extras passed")

    override fun onRunJob(params: Params): Result {
        if (StorageHelper.user == null) {
            return Result.FAILURE
        }

        synchronized(lock, {
            when {
                System.currentTimeMillis() > lastTime + 40 * 1000 -> {
                    lastTime = System.currentTimeMillis()
                    ongoing = 1
                }
                ongoing <= 6 -> ongoing++
                else -> return Result.RESCHEDULE
            }
        })

        try {
            bus.post(StartedEvent())
            mangaDao.insertEntry(api.info().entryCore(entryId).build().execute().toLocalEntryCore())

            val chapter = api.manga().chapter(entryId, episode, language).build().execute()
            val pages = chapter.pages.map { it.toLocalPage(chapterId = chapter.id.toLong()) }

            for (it in chapter.pages) {
                if (isCanceled) {
                    return Result.FAILURE
                }

                MangaPageSingle(context, true, Input(chapter.server, entryId, chapter.id, it.decodedName))
                        .subscribe()
            }

            mangaDao.insertChapter(chapter.toLocalChapter(episode, language), pages)
            bus.post(FinishedEvent(entryId, episode, language))

            return Result.SUCCESS
        } catch (error: Throwable) {
            val isIpBlockedError = error is ProxerException && error.serverErrorType == ServerErrorType.IP_BLOCKED

            return if (isIpBlockedError || params.failureCount >= 1) {
                bus.post(FailedEvent(entryId, episode, language))

//                MangaNotificationHelper.showError(context, error)

                Result.FAILURE
            } else {
                Result.RESCHEDULE
            }
        }
    }

    private fun isJobFor(entryId: String, episode: Int, language: Language): Boolean {
        return this.entryId == entryId && this.episode == episode && this.language == language
    }

    class StartedEvent
    class FinishedEvent(val entryId: String, val episode: Int, val language: Language)
    class FailedEvent(val entryId: String, val episode: Int, val language: Language)
}
