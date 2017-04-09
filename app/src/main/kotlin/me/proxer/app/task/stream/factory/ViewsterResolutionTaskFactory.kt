package me.proxer.app.task.stream.factory

import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.task.stream.UrlEchoTask

/**
 * @author Ruben Gees
 */
class ViewsterResolutionTaskFactory : HosterResolutionTaskFactory() {

    override val name = "Viewster"
    override fun create() = TaskBuilder.task(UrlEchoTask()).build()
}
