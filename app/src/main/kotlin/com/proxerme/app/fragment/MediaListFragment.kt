package com.proxerme.app.fragment

import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.proxerme.app.R
import com.proxerme.app.adapter.MediaAdapter
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.ProxerConnection
import com.proxerme.library.connection.list.request.MediaSearchRequest
import com.proxerme.library.info.ProxerTag
import com.proxerme.library.parameters.CategoryParameter
import com.proxerme.library.parameters.MediaSortParameter
import com.proxerme.library.parameters.TypeParameter

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MediaListFragment : PagingFragment() {

    companion object {
        private const val ARGUMENT_CATEGORY = "category"
        private const val STATE_SORT_CRITERIA = "state_sort_criteria"

        fun newInstance(@CategoryParameter.Category category: String): MediaListFragment {
            return MediaListFragment().apply {
                this.arguments = Bundle().apply {
                    this.putString(ARGUMENT_CATEGORY, category)
                }
            }
        }
    }

    override val section = SectionManager.Section.MEDIA_LIST

    @CategoryParameter.Category
    private lateinit var category: String

    @MediaSortParameter.MediaSortCriteria
    private lateinit var sortCriteria: String

    private lateinit var adapter: MediaAdapter
    override lateinit var layoutManager: StaggeredGridLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        category = arguments.getString(ARGUMENT_CATEGORY)

        if (savedInstanceState != null) {
            sortCriteria = savedInstanceState.getString(STATE_SORT_CRITERIA)
        } else {
            sortCriteria = MediaSortParameter.RATING
        }

        adapter = MediaAdapter(savedInstanceState, category, sortCriteria)
        layoutManager = StaggeredGridLayoutManager(Utils.calculateSpanAmount(activity) + 1,
                StaggeredGridLayoutManager.VERTICAL)

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_media_list, menu)

        when (sortCriteria) {
            MediaSortParameter.RATING -> menu.findItem(R.id.rating).isChecked = true
            MediaSortParameter.COUNT -> menu.findItem(R.id.count).isChecked = true
            MediaSortParameter.NAME -> menu.findItem(R.id.name).isChecked = true
        }

        super.onCreateOptionsMenu(menu, inflater)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val previousCriteria = sortCriteria

        when (item.itemId) {
            R.id.rating -> sortCriteria = MediaSortParameter.RATING
            R.id.count -> sortCriteria = MediaSortParameter.COUNT
            R.id.name -> sortCriteria = MediaSortParameter.NAME
            else -> return false
        }

        if (previousCriteria != sortCriteria) {
            reset()
            item.isChecked = true
        }

        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.setHasFixedSize(true)
        list.layoutManager = layoutManager
        list.adapter = adapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(STATE_SORT_CRITERIA, sortCriteria)
        adapter.saveInstanceState(outState)
    }

    override fun loadPage(number: Int) {
        MediaSearchRequest(number)
                .withType(getTypeParameter())
                .withSortCriteria(sortCriteria)
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

    override fun reset() {
        adapter.sortCriteria = sortCriteria

        super.reset()
    }

    @TypeParameter.Type
    private fun getTypeParameter(): String {
        return when (category) {
            CategoryParameter.ANIME -> TypeParameter.ALL_ANIME
            CategoryParameter.MANGA -> TypeParameter.ALL_MANGA
            else -> throw RuntimeException("Invalid category")
        }
    }
}