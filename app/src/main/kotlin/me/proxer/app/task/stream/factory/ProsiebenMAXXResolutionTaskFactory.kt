package me.proxer.app.task.stream.factory

import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.task.stream.UrlEchoTask

/**
 * @author Ruben Gees
 */
class ProsiebenMAXXResolutionTaskFactory : HosterResolutionTaskFactory() {

    override val name = "ProSieben MAXX"
    override fun create() = TaskBuilder.task(UrlEchoTask()).build()
}
