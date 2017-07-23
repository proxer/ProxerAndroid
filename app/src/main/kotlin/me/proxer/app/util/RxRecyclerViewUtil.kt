package me.proxer.app.util

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import com.jakewharton.rxbinding2.support.v7.widget.RxRecyclerView
import io.reactivex.Observable

/**
 * @author Ruben Gees
 */
object RxRecyclerViewUtil {

    fun endScrolls(view: RecyclerView, threshold: Int): Observable<Unit> = RxRecyclerView.scrollEvents(view)
            .filter {
                view.layoutManager.let {
                    val pastVisibleItems = when (it) {
                        is StaggeredGridLayoutManager -> {
                            IntArray(it.spanCount).apply {
                                it.findFirstVisibleItemPositions(this)
                            }.let { firstVisibleItems ->
                                when (firstVisibleItems.isNotEmpty()) {
                                    true -> firstVisibleItems[0]
                                    false -> 0
                                }
                            }
                        }
                        is LinearLayoutManager -> it.findFirstVisibleItemPosition()
                        else -> 0
                    }

                    it.itemCount > 0 && it.childCount + pastVisibleItems >= it.itemCount - threshold
                }
            }
            .map { Unit }
}
