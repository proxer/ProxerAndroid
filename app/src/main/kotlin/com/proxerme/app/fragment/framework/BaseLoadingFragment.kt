package com.proxerme.app.fragment.framework

import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import com.proxerme.app.R
import com.proxerme.app.util.KotterKnife
import com.proxerme.app.util.bindView

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class BaseLoadingFragment : MainFragment() {

    open protected val isSwipeToRefreshEnabled = false

    open protected val progress: SwipeRefreshLayout by bindView(R.id.progress)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progress.setColorSchemeResources(R.color.primary, R.color.accent)
    }

    override fun onDestroyView() {
        KotterKnife.reset(this)

        super.onDestroyView()
    }

    protected fun setRefreshing(enable: Boolean) {
        progress.isEnabled = if (!enable) isSwipeToRefreshEnabled else true
        progress.isRefreshing = enable
    }
}