package com.proxerme.app.task.framework

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
interface Task<O> {

    val isWorking: Boolean

    fun execute(successCallback: (O) -> Unit, exceptionCallback: (Exception) -> Unit)

    fun cancel()
    fun reset()
    fun destroy()
}