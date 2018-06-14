package me.proxer.app.media.list

import android.arch.lifecycle.Observer
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetBehavior.STATE_COLLAPSED
import android.support.design.widget.BottomSheetBehavior.STATE_EXPANDED
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.checkedChanges
import me.proxer.app.R
import me.proxer.app.util.extension.ProxerLibExtensions
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.dip
import me.proxer.app.util.extension.enableLayoutAnimationsSafely
import me.proxer.app.util.extension.enumSetOf
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.toAppString
import me.proxer.library.enums.FskConstraint
import me.proxer.library.enums.Language
import me.proxer.library.enums.TagRateFilter
import me.proxer.library.enums.TagSpoilerFilter

/**
 * @author Ruben Gees
 */
class MediaListSearchBottomSheet private constructor(
    private val fragment: MediaListFragment,
    private val viewModel: MediaListViewModel
) {

    companion object {
        fun bindTo(
            fragment: MediaListFragment,
            viewModel: MediaListViewModel
        ) = MediaListSearchBottomSheet(fragment, viewModel)
    }

    private val bottomSheetBehaviour = BottomSheetBehavior.from(fragment.searchBottomSheet)

    init {
        fragment.languageSelector.findViewById<ViewGroup>(R.id.items).enableLayoutAnimationsSafely()
        fragment.genreSelector.findViewById<ViewGroup>(R.id.items).enableLayoutAnimationsSafely()
        fragment.excludedGenreSelector.findViewById<ViewGroup>(R.id.items).enableLayoutAnimationsSafely()
        fragment.fskSelector.findViewById<ViewGroup>(R.id.items).enableLayoutAnimationsSafely()
        fragment.tagSelector.findViewById<ViewGroup>(R.id.items).enableLayoutAnimationsSafely()
        fragment.excludedTagSelector.findViewById<ViewGroup>(R.id.items).enableLayoutAnimationsSafely()

        initClickSubscriptions()
        initSelectionSubscriptions()

        fragment.includeUnratedTags.checkedChanges()
            .skipInitialValue()
            .autoDispose(fragment)
            .subscribe { fragment.tagRateFilter = if (it) TagRateFilter.ALL else TagRateFilter.RATED_ONLY }

        fragment.includeSpoilerTags.checkedChanges()
            .skipInitialValue()
            .autoDispose(fragment)
            .subscribe { fragment.tagSpoilerFilter = if (it) TagSpoilerFilter.ALL else TagSpoilerFilter.NO_SPOILERS }

        val fskItems = FskConstraint.values().map { it.toAppString(fragment.requireContext()) }
        val languageItems = listOf(
            fragment.getString(R.string.fragment_media_list_all_languages),
            fragment.getString(R.string.language_german),
            fragment.getString(R.string.language_english)
        )

        fragment.languageSelector.items = languageItems
        fragment.fskSelector.items = fskItems

        fragment.searchBottomSheetTitle.post {
            if (fragment.view != null) {
                bottomSheetBehaviour.peekHeight = fragment.searchBottomSheetTitle.height + fragment.dip(10)
                fragment.searchBottomSheet.visibility = View.VISIBLE
            }
        }

        viewModel.loadTags()

        viewModel.genreData.observe(fragment, Observer {
            if (it != null) {
                fragment.genreSelector.items = it.map { it.name }
                fragment.excludedGenreSelector.items = it.map { it.name }

                fragment.genreSelector.visibility = View.VISIBLE
                fragment.excludedGenreSelector.visibility = View.VISIBLE
            }
        })

        viewModel.tagData.observe(fragment, Observer {
            if (it != null) {
                fragment.tagSelector.items = it.map { it.name }
                fragment.excludedTagSelector.items = it.map { it.name }

                fragment.tagSelector.visibility = View.VISIBLE
                fragment.excludedTagSelector.visibility = View.VISIBLE
            }
        })
    }

    private fun initClickSubscriptions() {
        fragment.searchBottomSheetTitle.clicks()
            .autoDispose(fragment)
            .subscribe {
                bottomSheetBehaviour.state = when (bottomSheetBehaviour.state) {
                    STATE_EXPANDED -> STATE_COLLAPSED
                    else -> STATE_EXPANDED
                }
            }

        fragment.search.clicks()
            .autoDispose(fragment)
            .subscribe {
                fragment.searchView.clearFocus()

                bottomSheetBehaviour.state = STATE_COLLAPSED

                viewModel.reload()
            }
    }

    private fun initSelectionSubscriptions() {
        fragment.languageSelector.selectionChangeSubject
            .autoDispose(fragment)
            .subscribeAndLogErrors {
                fragment.language = when {
                    it.firstOrNull() == fragment.getString(R.string.language_german) -> Language.GERMAN
                    it.firstOrNull() == fragment.getString(R.string.language_english) -> Language.ENGLISH
                    else -> null
                }
            }

        fragment.genreSelector.selectionChangeSubject
            .autoDispose(fragment)
            .subscribeAndLogErrors { selections ->
                viewModel.genreData.value?.let { genreData ->
                    fragment.genres = selections.mapNotNull { selection -> genreData.find { it.name == selection } }
                }
            }

        fragment.excludedGenreSelector.selectionChangeSubject
            .autoDispose(fragment)
            .subscribeAndLogErrors { selections ->
                viewModel.genreData.value?.let { genreData ->
                    fragment.excludedGenres = selections.mapNotNull { selection ->
                        genreData.find { it.name == selection }
                    }
                }
            }

        fragment.fskSelector.selectionChangeSubject
            .autoDispose(fragment)
            .subscribeAndLogErrors { selections ->
                fragment.fskConstraints = enumSetOf(selections.map {
                    ProxerLibExtensions.fskConstraintFromAppString(fragment.requireContext(), it)
                })
            }

        fragment.tagSelector.selectionChangeSubject
            .autoDispose(fragment)
            .subscribeAndLogErrors { selections ->
                viewModel.tagData.value?.let { tagData ->
                    fragment.tags = selections.mapNotNull { selection -> tagData.find { it.name == selection } }
                }
            }

        fragment.excludedTagSelector.selectionChangeSubject
            .autoDispose(fragment)
            .subscribeAndLogErrors { selections ->
                viewModel.tagData.value?.let { tagData ->
                    fragment.excludedTags = selections.mapNotNull { selection -> tagData.find { it.name == selection } }
                }
            }
    }

    fun onBackPressed() = if (bottomSheetBehaviour.state != STATE_COLLAPSED) {
        bottomSheetBehaviour.state = STATE_COLLAPSED

        true
    } else {
        false
    }
}
