package me.proxer.app.task

import com.rubengees.ktask.base.BranchTask
import com.rubengees.ktask.base.Task

/**
 * @author Ruben Gees
 */
class PagedTask<I, O>(override val innerTask: Task<I, O>) : BranchTask<Pair<Int, I>, Pair<Int, O>, I, O>() {

    private var currentPage: Int? = null

    init {
        restoreCallbacks(this)
    }

    override fun execute(input: Pair<Int, I>) {
        start {
            currentPage = input.first

            innerTask.execute(input.second)
        }
    }

    override fun cancel() {
        super.cancel()

        currentPage = null
    }

    override fun restoreCallbacks(from: Task<Pair<Int, I>, Pair<Int, O>>) {
        super.restoreCallbacks(from)

        innerTask.onSuccess {
            val safePage = currentPage

            if (safePage != null) {
                finishSuccessful(safePage to it)
            } else {
                finishWithError(IllegalStateException("safePage is null"))
            }

            currentPage = null
        }

        innerTask.onError {
            val safePage = currentPage

            if (safePage != null) {
                finishWithError(PagedException(it, safePage))
            } else {
                finishWithError(IllegalStateException("safePage is null"))
            }

            currentPage = null
        }
    }

    class PagedException(val innerError: Throwable, val page: Int) : Exception()
}
