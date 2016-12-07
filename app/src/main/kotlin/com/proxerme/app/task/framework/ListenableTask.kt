package com.proxerme.app.task.framework

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
interface ListenableTask<I, O> : Task<I, O> {

    fun onStart(callback: () -> Unit): ListenableTask<I, O>

    fun onSuccess(callback: () -> Unit): ListenableTask<I, O>

    fun onException(callback: () -> Unit): ListenableTask<I, O>

    fun onFinish(callback: () -> Unit): ListenableTask<I, O>

}