package com.proxerme.app.task.framework

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class InputEchoTask<I, O>(private val task: Task<I, O>,
                          successCallback: ((Pair<I, O>) -> Unit)? = null,
                          exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<I, Pair<I, O>>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = task.isWorking

    private var currentInput: I? = null

    init {
        task.successCallback = {
            val safeInput = currentInput

            if (safeInput != null) {
                finishSuccessful(safeInput to it)
            } else {
                finishWithException(IllegalStateException())
            }

            currentInput = null
        }

        task.exceptionCallback = {
            finishWithException(it)

            currentInput = null
        }
    }

    override fun execute(input: I) {
        start {
            currentInput = input

            task.execute(input)
        }
    }

    override fun cancel() {
        currentInput = null

        task.cancel()
    }

    override fun reset() {
        task.reset()
    }

    override fun destroy() {
        task.destroy()

        super.destroy()
    }

    override fun onStart(callback: () -> Unit): BaseTask<I, Pair<I, O>> {
        task.onStart(callback)

        return this
    }
}