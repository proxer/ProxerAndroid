package com.proxerme.app.task

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
interface Task<O> {

    val isWorking: Boolean

    fun onStart(callback: () -> Unit): Task<O>
    fun onSuccess(callback: () -> Unit): Task<O>
    fun onException(callback: () -> Unit): Task<O>
    fun onFinish(callback: () -> Unit): Task<O>

    fun execute(successCallback: (O) -> Unit, exceptionCallback: (Exception) -> Unit)

    fun cancel()
    fun reset()
    fun destroy()
}