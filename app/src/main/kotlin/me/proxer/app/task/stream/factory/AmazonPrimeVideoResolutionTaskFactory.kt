package me.proxer.app.task.stream.factory

import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.task.stream.UrlEchoTask

/**
 * @author Ruben Gees
 */
class AmazonPrimeVideoResolutionTaskFactory : HosterResolutionTaskFactory() {

    override val name = "Amazon Prime Video"
    override fun create() = TaskBuilder.task(UrlEchoTask()).build()
}
