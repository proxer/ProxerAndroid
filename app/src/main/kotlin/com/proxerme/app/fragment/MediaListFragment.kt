package com.proxerme.app.fragment

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.View
import com.proxerme.app.adapter.MediaAdapter
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.ProxerConnection
import com.proxerme.library.connection.list.request.MediaListRequest
import com.proxerme.library.connection.parameters.CategoryParameter.Category
import com.proxerme.library.info.ProxerTag

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MediaListFragment : PagingFragment() {

    companion object {
        private const val ARGUMENT_CATEGORY = "category"

        fun newInstance(@Category category: String): MediaListFragment {
            return MediaListFragment().apply {
                this.arguments = Bundle().apply {
                    this.putString(ARGUMENT_CATEGORY, category)
                }
            }
        }
    }

    override val section = SectionManager.Section.MEDIA_LIST

    @Category
    private lateinit var category: String

    private lateinit var adapter: MediaAdapter
    override lateinit var layoutManager: GridLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        category = arguments.getString(ARGUMENT_CATEGORY)

        adapter = MediaAdapter(savedInstanceState, category)
        layoutManager = GridLayoutManager(context, Utils.calculateSpanAmount(activity) + 1)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.setHasFixedSize(true)
        list.layoutManager = layoutManager
        list.adapter = adapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }

    override fun loadPage(number: Int) {
        MediaListRequest(number)
                .withCategory(category)
                .withLimit(50)
                .execute({ result ->
                    adapter.addItems(result.item.toList())

                    notifyPagedLoadFinishedSuccessful(number, result)
                }, { result ->
                    notifyPagedLoadFinishedWithError(number, result)
                })
    }

    override fun cancel() {
        ProxerConnection.cancel(ProxerTag.MEDIA_LIST)
    }

    override fun clear() {
        adapter.clear()
    }
}