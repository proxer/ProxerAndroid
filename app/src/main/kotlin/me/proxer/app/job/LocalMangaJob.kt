package me.proxer.app.job

import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.application.MainApplication.Companion.mangaDb
import me.proxer.app.event.LocalMangaJobFailedEvent
import me.proxer.app.event.LocalMangaJobFinishedEvent
import me.proxer.app.helper.NotificationHelper
import me.proxer.app.util.Utils
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

        private const val ID_EXTRA = "id"
        private const val EPISODE_EXTRA = "episode"
        private const val LANGUAGE_EXTRA = "language"
        private const val THIRTY_MINUTES = 1000L * 60 * 30

        fun schedule(id: String, episode: Int, language: Language) {
            val extras = PersistableBundleCompat().apply {
                putString(ID_EXTRA, id)
                putInt(EPISODE_EXTRA, episode)
                putString(LANGUAGE_EXTRA, ProxerUtils.getApiEnumName(language))
            }

            JobRequest.Builder(constructTag(id, episode, language))
                    .setExtras(extras)
                    .setRequiredNetworkType(JobRequest.NetworkType.UNMETERED)
                    .setExecutionWindow(1L, THIRTY_MINUTES)
                    .setRequirementsEnforced(true)
                    .setUpdateCurrent(true)
                    .setPersisted(true)
                    .build()
                    .schedule()
        }

        fun cancel(id: String, episode: Int, language: Language) {
            JobManager.instance().cancelAllForTag(constructTag(id, episode, language))
        }

        fun constructTag(id: String, episode: Int, language: Language): String {
            return "${TAG}_${id}_${episode}_${ProxerUtils.getApiEnumName(language)}"
        }
    }

    val id: String
        get() = params.extras.getString(ID_EXTRA, "-1") ?: throw IllegalArgumentException("No extras passed")

    val episode: Int
        get() = params.extras.getInt(EPISODE_EXTRA, -1)

    val language: Language
        get() = ProxerUtils.toApiEnum(Language::class.java, params.extras.getString(LANGUAGE_EXTRA, "en"))
                ?: throw IllegalArgumentException("No extras passed")

    override fun onRunJob(params: Params): Result {
        try {
            if (mangaDb.findEntry(id) == null) {
                mangaDb.insertEntry(api.info().entryCore(id).build().execute())
            }

            val chapter = api.manga().chapter(id, episode, language).build().execute()

            for (it in chapter.pages) {
                if (isCanceled) {
                    EventBus.getDefault().post(LocalMangaJobFailedEvent(id, episode, language))

                    return Result.FAILURE
                }

                Utils.getBitmapFromUrl(context, ProxerUrls.mangaPageImage(chapter.server, id,
                        chapter.id, it.name).toString()) ?: throw RuntimeException("Page download failed")
            }

            mangaDb.insertChapter(chapter, episode, language)

            EventBus.getDefault().post(LocalMangaJobFinishedEvent(id, episode, language))

            return Result.SUCCESS
        } catch (error: Throwable) {
            EventBus.getDefault().post(LocalMangaJobFailedEvent(id, episode, language))
            NotificationHelper.showMangaDownloadErrorNotification(context)

            return Result.FAILURE
        }
    }

    fun isJobFor(id: String, episode: Int, language: Language): Boolean {
        return this.id == id && this.episode == episode && this.language == language
    }

    fun isCanceledPublic(): Boolean {
        return isCanceled
    }
}
