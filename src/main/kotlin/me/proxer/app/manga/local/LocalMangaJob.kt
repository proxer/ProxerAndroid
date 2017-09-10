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
import me.proxer.app.MainApplication.Companion.mangaDatabase
import me.proxer.app.manga.MangaPageSingle
import me.proxer.app.manga.MangaPageSingle.Input
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.decodedName
import me.proxer.app.util.extension.toLocalChapter
import me.proxer.app.util.extension.toLocalEntryCore
import me.proxer.app.util.extension.toLocalPage
import me.proxer.library.enums.Language
import me.proxer.library.util.ProxerUtils
import org.threeten.bp.LocalDateTime
import java.util.concurrent.CancellationException

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
        private var lastTime = LocalDateTime.now()
        private var ongoing = 0

        private var maxBatchProgress = 0.0
        private var batchProgress = 0.0

        fun schedule(context: Context, entryId: String, episode: Int, language: Language) {
            val isUnmeteredRequired = PreferenceHelper.isUnmeteredNetworkRequiredForMangaDownload(context)
            val extras = PersistableBundleCompat().apply {
                putString(ENTRY_ID_EXTRA, entryId)
                putInt(EPISODE_EXTRA, episode)
                putString(LANGUAGE_EXTRA, ProxerUtils.getApiEnumName(language))
            }

            maxBatchProgress += 100F
            LocalMangaNotifications.showOrUpdate(context, maxBatchProgress, batchProgress)

            JobRequest.Builder(constructTag(entryId, episode, language))
                    .setExtras(extras)
                    .setRequiredNetworkType(if (isUnmeteredRequired) NetworkType.UNMETERED else NetworkType.CONNECTED)
                    .setBackoffCriteria(15000, JobRequest.BackoffPolicy.LINEAR)
                    .setExecutionWindow(1L, 100L)
                    .setRequirementsEnforced(true)
                    .setUpdateCurrent(true)
                    .build()
                    .schedule()
        }

        fun cancel(entryId: String, episode: Int, language: Language) {
            JobManager.instance().cancelAllForTag(constructTag(entryId, episode, language))
        }

        fun cancelAll() = JobManager.instance().allJobRequests.filter { it.tag.startsWith(TAG) }
                .map { it.tag }
                .plus(JobManager.instance().allJobs.filter { it is LocalMangaJob }.map {
                    (it as LocalMangaJob).let { constructTag(it.entryId, it.episode, it.language) }
                })
                .forEach { JobManager.instance().cancelAllForTag(it) }

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
        return try {
            if (StorageHelper.user == null) Result.FAILURE

            synchronized(lock, {
                when {
                    LocalDateTime.now().isAfter(lastTime.plusSeconds(40)) -> {
                        lastTime = LocalDateTime.now()
                        ongoing = 1
                    }
                    ongoing <= 6 -> ongoing++
                    else -> Result.RESCHEDULE
                }
            })

            bus.post(StartedEvent())

            loadChapterAndPages()

            bus.post(FinishedEvent(entryId, episode, language))

            Result.SUCCESS
        } catch (error: Throwable) {
            when {
                ErrorUtils.isIpBlockedError(error) || params.failureCount >= 1 -> {
                    cancelAll()

                    bus.post(FailedEvent(entryId, episode, language))

                    LocalMangaNotifications.cancel(context)
                    LocalMangaNotifications.showError(context, error)

                    Result.FAILURE
                }
                isCanceled -> {
                    bus.post(FailedEvent(entryId, episode, language))

                    LocalMangaNotifications.cancel(context)

                    Result.FAILURE
                }
                else -> Result.RESCHEDULE
            }
        } finally {
            if (countRunningJobs() + countScheduledJobs() <= 1) {
                maxBatchProgress = 0.0
                batchProgress = 0.0
            }
        }
    }

    private fun loadChapterAndPages() {
        val entry = when (mangaDao.countEntries(entryId.toLong()) <= 0) {
            true -> api.info().entryCore(entryId).build().safeExecute().toLocalEntryCore()
            false -> null
        }

        val chapter = api.manga().chapter(entryId, episode, language).build().safeExecute()
        val pages = chapter.pages.map { it.toLocalPage(chapterId = chapter.id.toLong()) }
        var error: Throwable? = null

        pages.forEach { page ->
            MangaPageSingle(context, true, Input(chapter.server, entryId, chapter.id, page.decodedName))
                    .subscribe({}, { error = it })

            error?.let { throw it }
            if (isCanceled) throw CancellationException()

            batchProgress += 100F / pages.size
            LocalMangaNotifications.showOrUpdate(context, maxBatchProgress, batchProgress)
        }

        mangaDatabase.runInTransaction {
            entry?.let { entry -> mangaDao.insertEntry(entry) }
            mangaDao.insertChapterAndPages(chapter.toLocalChapter(episode, language), pages)
        }
    }

    private fun isJobFor(entryId: String, episode: Int, language: Language): Boolean {
        return this.entryId == entryId && this.episode == episode && this.language == language
    }

    class StartedEvent
    class FinishedEvent(val entryId: String, val episode: Int, val language: Language)
    class FailedEvent(val entryId: String, val episode: Int, val language: Language)
}
