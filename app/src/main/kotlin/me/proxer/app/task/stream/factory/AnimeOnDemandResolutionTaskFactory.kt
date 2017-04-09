package me.proxer.app.task.stream.factory

import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.task.stream.UrlEchoTask

/**
 * @author Ruben Gees
 */
class AnimeOnDemandResolutionTaskFactory : HosterResolutionTaskFactory() {

    override val name = "Anime on demand"
    override fun create() = TaskBuilder.task(UrlEchoTask()).build()
}
