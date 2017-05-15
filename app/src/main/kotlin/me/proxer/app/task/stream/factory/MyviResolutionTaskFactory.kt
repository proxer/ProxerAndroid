package me.proxer.app.task.stream.factory

import com.rubengees.ktask.util.TaskBuilder
import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.application.MainApplication.Companion.moshi
import me.proxer.app.task.stream.LinkResolutionTask
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionException
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionResult
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.androidUri
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
class MyviResolutionTaskFactory : HosterResolutionTaskFactory() {

    private companion object {
        private val urlTransformation = { url: String ->
            HttpUrl.parse("http://myvi.ru/player/api/Video/Get/${Utils.parseAndFixUrl(url).pathSegments().last()}?sig")
                    ?: throw NullPointerException()
        }
    }

    override val name = "Myvi"
    override fun create() = TaskBuilder.task(LinkResolutionTask(urlTransformation = urlTransformation))
            .then(MyviTask())
            .build()

    class MyviTask : WorkerTask<String, StreamResolutionResult>() {

        override fun work(input: String): StreamResolutionResult {
            val resultUrl = moshi.adapter(SprutoResult::class.java).fromJson(input)?.url
                    ?: throw  StreamResolutionException()

            return StreamResolutionResult(resultUrl.androidUri(), "video/mp4")
        }

        private class SprutoResult(private val sprutoData: SprutoData) {
            val url: HttpUrl? get() = HttpUrl.parse(sprutoData.playlist?.first()?.video?.first()?.url)
        }

        private class SprutoData(val playlist: Array<SprutoPlaylistItem>?)
        private class SprutoPlaylistItem(val video: Array<SprutoVideo>?)
        private class SprutoVideo(val url: String?)
    }
}
