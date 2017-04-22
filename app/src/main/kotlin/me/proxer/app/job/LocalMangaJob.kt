package me.proxer.app.job

import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.application.MainApplication.Companion.mangaDb
import me.proxer.app.entity.MangaChapterInfo
import me.proxer.app.util.Utils
import me.proxer.library.enums.Language
import me.proxer.library.util.ProxerUrls
import me.proxer.library.util.ProxerUtils

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

        fun constructTag(id: String, episode: Int, language: Language): String {
            return "${TAG}_${id}_${episode}_${ProxerUtils.getApiEnumName(language)}"
        }
    }

    var tag: String? = null

    override fun onRunJob(params: Params): Result {
        try {
            val id = params.extras.getString(ID_EXTRA, "-1") ?: throw IllegalArgumentException("No extras passed")
            val episode = params.extras.getInt(EPISODE_EXTRA, -1)
            val language = ProxerUtils.toApiEnum(Language::class.java, params.extras.getString(LANGUAGE_EXTRA, "en"))
                    ?: throw IllegalArgumentException("No extras passed")

            tag = constructTag(id, episode, language)

            val entryInfo = api.info().entryCore(id).build().execute()
            val chapterInfo = MangaChapterInfo(api.manga().chapter(id, episode, language).build().execute(),
                    entryInfo.name, entryInfo.episodeAmount)

            chapterInfo.chapter.pages.forEach {
                Utils.getBitmapFromUrl(context, ProxerUrls.mangaPageImage(chapterInfo.chapter.server, id,
                        chapterInfo.chapter.id, it.name).toString()) ?: throw RuntimeException("Page download failed")
            }

            mangaDb.insert(chapterInfo, episode, language)

            return Result.SUCCESS
        } catch (error: Throwable) {
            return Result.FAILURE
        }
    }

    fun isJobFor(tag: String): Boolean {
        return this.tag == tag
    }
}
