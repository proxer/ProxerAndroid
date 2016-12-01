package com.proxerme.app.task.framework

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
interface ListenableTask<O> : Task<O> {

    fun onStart(callback: () -> Unit): ListenableTask<O>

    fun onSuccess(callback: () -> Unit): ListenableTask<O>

    fun onException(callback: () -> Unit): ListenableTask<O>

    fun onFinish(callback: () -> Unit): ListenableTask<O>

}