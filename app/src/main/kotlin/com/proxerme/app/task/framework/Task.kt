package com.proxerme.app.task.framework

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
interface Task<I, O> {

    val isWorking: Boolean
    var successCallback: ((O) -> Unit)?
    var exceptionCallback: ((Exception) -> Unit)?

    fun execute(input: I)

    fun cancel()
    fun reset()
    fun destroy()

    fun onStart(callback: () -> Unit): Task<I, O>
    fun onSuccess(callback: () -> Unit): Task<I, O>
    fun onException(callback: () -> Unit): Task<I, O>
    fun onFinish(callback: () -> Unit): Task<I, O>
}