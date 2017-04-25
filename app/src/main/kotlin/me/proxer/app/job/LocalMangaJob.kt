package me.proxer.app.job

import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.application.MainApplication.Companion.mangaDb
import me.proxer.app.event.LocalMangaJobFailedEvent
import me.proxer.app.event.LocalMangaJobFinishedEvent
import me.proxer.app.helper.NotificationHelper
import me.proxer.app.util.extension.decodedName
import me.proxer.library.enums.Language
import me.proxer.library.util.ProxerUrls
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

        fun schedule(entryId: String, episode: Int, language: Language) {
            val extras = PersistableBundleCompat().apply {
                putString(ENTRY_ID_EXTRA, entryId)
                putInt(EPISODE_EXTRA, episode)
                putString(LANGUAGE_EXTRA, ProxerUtils.getApiEnumName(language))
            }

            JobRequest.Builder(constructTag(entryId, episode, language))
                    .setExtras(extras)
                    .setRequiredNetworkType(JobRequest.NetworkType.UNMETERED)
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
        try {
            if (!mangaDb.containsEntry(entryId)) {
                mangaDb.insertEntry(api.info().entryCore(entryId).build().execute())
            }

            val chapter = api.manga().chapter(entryId, episode, language).build().execute()

            for (it in chapter.pages) {
                if (isCanceled) {
                    return Result.FAILURE
                }

                Glide.with(context)
                        .load(ProxerUrls.mangaPageImage(chapter.server, entryId, chapter.id, it.decodedName).toString())
                        .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .get()
            }

            mangaDb.insertChapter(chapter, episode, language)

            EventBus.getDefault().post(LocalMangaJobFinishedEvent(entryId, episode, language))

            return Result.SUCCESS
        } catch (error: Throwable) {
            EventBus.getDefault().post(LocalMangaJobFailedEvent(entryId, episode, language))

            NotificationHelper.showMangaDownloadErrorNotification(context, error)

            return Result.FAILURE
        }
    }

    private fun isJobFor(entryId: String, episode: Int, language: Language): Boolean {
        return this.entryId == entryId && this.episode == episode && this.language == language
    }
}
