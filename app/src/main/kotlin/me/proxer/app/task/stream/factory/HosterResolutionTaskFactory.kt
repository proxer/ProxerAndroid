package me.proxer.app.task.stream.factory

import com.rubengees.ktask.base.Task
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionResult

/**
 * @author Ruben Gees
 */
abstract class HosterResolutionTaskFactory {

    abstract val name: String
    open val supports: (name: String) -> Boolean = { name -> name.equals(this.name, true) }

    abstract fun create(): Task<String, StreamResolutionResult>
}