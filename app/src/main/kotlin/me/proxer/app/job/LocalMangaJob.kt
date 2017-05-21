package me.proxer.app.job

import android.content.Context
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.evernote.android.job.JobRequest.NetworkType
import com.evernote.android.job.util.support.PersistableBundleCompat
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.application.MainApplication.Companion.mangaDb
import me.proxer.app.event.manga.LocalMangaJobFailedEvent
import me.proxer.app.event.manga.LocalMangaJobFinishedEvent
import me.proxer.app.event.manga.LocalMangaJobStartedEvent
import me.proxer.app.helper.NotificationHelper
import me.proxer.app.helper.PreferenceHelper
import me.proxer.app.helper.StorageHelper
import me.proxer.app.task.manga.MangaPageDownloadTask
import me.proxer.app.task.manga.MangaPageDownloadTask.MangaPageDownloadTaskInput
import me.proxer.app.util.extension.decodedName
import me.proxer.library.enums.Language
import me.proxer.library.util.ProxerUtils
import org.greenrobot.eventbus.EventBus

/**
 * @author Ruben Gees
 */
class LocalMangaJob : Job() {

    companion object {
        const val TAG = "local_manga_job"

        private const val ENTRY_ID_EXTRA = "entry_id"
        private const val EPISODE_EXTRA = "episode"
        private const val LANGUAGE_EXTRA = "language"

        fun schedule(context: Context, entryId: String, episode: Int, language: Language) {
            val unmeteredRequired = PreferenceHelper.isUnmeteredNetworkRequiredForMangaDownload(context)
            val extras = PersistableBundleCompat().apply {
                putString(ENTRY_ID_EXTRA, entryId)
                putInt(EPISODE_EXTRA, episode)
                putString(LANGUAGE_EXTRA, ProxerUtils.getApiEnumName(language))
            }

            val startTime = countRunningJobs() * 6000L + 1L
            val endTime = startTime + 100L

            JobRequest.Builder(constructTag(entryId, episode, language))
                    .setExtras(extras)
                    .setRequiredNetworkType(if (unmeteredRequired) NetworkType.UNMETERED else NetworkType.CONNECTED)
                    .setExecutionWindow(startTime, endTime)
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
            JobManager.instance().allJobRequests.filter { it.tag.startsWith(LocalMangaJob.TAG) }
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

        try {
            EventBus.getDefault().post(LocalMangaJobStartedEvent())

            if (!mangaDb.containsEntry(entryId)) {
                mangaDb.insertEntry(api.info().entryCore(entryId).build().execute())
            }

            val chapter = api.manga().chapter(entryId, episode, language).build().execute()
            val downloadTask = MangaPageDownloadTask(context.filesDir)

            for (it in chapter.pages) {
                if (isCanceled) {
                    return Result.FAILURE
                }

                downloadTask.serialExecute(MangaPageDownloadTaskInput(chapter.server, entryId,
                        chapter.id, it.decodedName))
            }

            mangaDb.insertChapter(chapter, episode, language)

            EventBus.getDefault().post(LocalMangaJobFinishedEvent(entryId, episode, language))

            return Result.SUCCESS
        } catch (error: Throwable) {
            if (params.failureCount <= 1) {
                return Result.RESCHEDULE
            } else {
                EventBus.getDefault().post(LocalMangaJobFailedEvent(entryId, episode, language))

                NotificationHelper.showMangaDownloadErrorNotification(context, error)

                return Result.FAILURE
            }
        }
    }

    private fun isJobFor(entryId: String, episode: Int, language: Language): Boolean {
        return this.entryId == entryId && this.episode == episode && this.language == language
    }
}
