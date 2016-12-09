package com.proxerme.app.task.framework

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
interface Task<in I, O> {

    val isWorking: Boolean
    var successCallback: ((O) -> Unit)?
    var exceptionCallback: ((Exception) -> Unit)?

    fun execute(input: I)

    fun cancel()
    fun reset()
    fun destroy()
}