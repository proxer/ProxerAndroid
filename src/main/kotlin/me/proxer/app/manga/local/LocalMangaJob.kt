package me.proxer.app.manga.local

import android.content.Context
import com.evernote.android.job.Job
import com.evernote.android.job.Job.Result.FAILURE
import com.evernote.android.job.Job.Result.RESCHEDULE
import com.evernote.android.job.Job.Result.SUCCESS
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.evernote.android.job.JobRequest.NetworkType
import com.evernote.android.job.util.support.PersistableBundleCompat
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.MainApplication.Companion.mangaDao
import me.proxer.app.MainApplication.Companion.mangaDatabase
import me.proxer.app.manga.MangaLocks
import me.proxer.app.manga.MangaPageSingle
import me.proxer.app.manga.MangaPageSingle.Input
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.decodedName
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.toLocalChapter
import me.proxer.app.util.extension.toLocalEntryCore
import me.proxer.app.util.extension.toLocalPage
import me.proxer.library.enums.Language
import me.proxer.library.util.ProxerUtils
import org.threeten.bp.LocalDateTime
import java.io.File
import java.util.concurrent.CancellationException
import kotlin.concurrent.write

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
            val isUnmeteredRequired = PreferenceHelper.isUnmeteredNetworkRequired(context)
            val extras = PersistableBundleCompat().apply {
                putString(ENTRY_ID_EXTRA, entryId)
                putInt(EPISODE_EXTRA, episode)
                putString(LANGUAGE_EXTRA, ProxerUtils.getApiEnumName(language))
            }

            maxBatchProgress += 100.0

            LocalMangaNotifications.showOrUpdate(context, maxBatchProgress, batchProgress,
                    countCurrentJobsForNotification())

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

        private fun countCurrentJobsForNotification(): Int {
            val currentJobs = countRunningJobs() + countScheduledJobs()

            return if (currentJobs == 0) 1 else currentJobs
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
        val result = try {
            val checkResult = checkAndUpdateState()

            if (checkResult != SUCCESS) {
                checkResult
            } else {
                bus.post(StartedEvent())

                loadChapterAndPages()

                bus.post(FinishedEvent(entryId, episode, language))

                SUCCESS
            }
        } catch (error: Throwable) {
            handleError(error, params)
        }

        val jobAmount = countRunningJobs() + countScheduledJobs()

        if (result == SUCCESS) {
            LocalMangaNotifications.showOrUpdate(context, maxBatchProgress, batchProgress, jobAmount - 1)
        }

        if (result != RESCHEDULE && jobAmount <= 1) {
            maxBatchProgress = 0.0
            batchProgress = 0.0
        }

        return result
    }

    private fun checkAndUpdateState() = when {
        !StorageHelper.isLoggedIn -> FAILURE
        else -> synchronized(lock, {
            when {
                LocalDateTime.now().isAfter(lastTime.plusSeconds(40)) -> {
                    lastTime = LocalDateTime.now()
                    ongoing = 1

                    SUCCESS
                }
                ongoing <= 6 -> {
                    ongoing++

                    SUCCESS
                }
                else -> RESCHEDULE
            }
        })
    }

    private fun handleError(error: Throwable, params: Params) = when {
        ErrorUtils.isIpBlockedError(error) || params.failureCount >= 1 -> {
            cancelAll()

            bus.post(FailedEvent(entryId, episode, language))

            LocalMangaNotifications.showError(context, error)

            FAILURE
        }
        isCanceled -> {
            bus.post(FailedEvent(entryId, episode, language))

            LocalMangaNotifications.cancel(context)

            FAILURE
        }
        else -> RESCHEDULE
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
                    .subscribeAndLogErrors({}, { error = it })

            error?.let { throw it }
            if (isCanceled) throw CancellationException()

            batchProgress += 100.0 / pages.size

            LocalMangaNotifications.showOrUpdate(context, maxBatchProgress, batchProgress,
                    countCurrentJobsForNotification())
        }

        MangaLocks.cacheLock.write {
            val cacheDirectory = File("${context.cacheDir}/manga/$entryId/${chapter.id}")

            if (cacheDirectory.exists()) {
                cacheDirectory.deleteRecursively()
            }
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
    data class FinishedEvent(val entryId: String, val episode: Int, val language: Language)
    data class FailedEvent(val entryId: String, val episode: Int, val language: Language)
}
