package me.proxer.app.media.list

import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetBehavior.STATE_COLLAPSED
import android.support.design.widget.BottomSheetBehavior.STATE_EXPANDED
import android.view.View
import android.view.View.MeasureSpec.makeMeasureSpec
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import me.proxer.app.R
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.dip
import me.proxer.app.util.extension.enableLayoutAnimationsSafely
import me.proxer.app.util.extension.enumSetOf
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.enums.Genre
import me.proxer.library.enums.Language
import me.proxer.library.util.ProxerUtils

/**
 * @author Ruben Gees
 */
class MediaListSearchBottomSheet private constructor(
        private val fragment: MediaListFragment,
        private val viewModel: MediaListViewModel,
        savedInstanceState: Bundle?
) {

    companion object {
        fun bindTo(
                fragment: MediaListFragment,
                viewModel: MediaListViewModel,
                savedInstanceState: Bundle?
        ) = MediaListSearchBottomSheet(fragment, viewModel, savedInstanceState)
    }

    private val bottomSheetBehaviour = BottomSheetBehavior.from(fragment.searchBottomSheet)

    init {
        if (savedInstanceState == null) {
            bottomSheetBehaviour.state = STATE_COLLAPSED
        }

        bottomSheetBehaviour.isHideable = false
        bottomSheetBehaviour.peekHeight = measureTitle()

        fragment.languageSelector.findViewById<ViewGroup>(R.id.items).enableLayoutAnimationsSafely()
        fragment.genreSelector.findViewById<ViewGroup>(R.id.items).enableLayoutAnimationsSafely()
        fragment.excludedGenreSelector.findViewById<ViewGroup>(R.id.items).enableLayoutAnimationsSafely()

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
                    bottomSheetBehaviour.state = STATE_COLLAPSED

                    viewModel.reload()
                }

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
                    fragment.genres = enumSetOf(selections.map {
                        toSafeApiEnum(Genre::class.java, it)
                    })
                }

        fragment.excludedGenreSelector.selectionChangeSubject
                .autoDispose(fragment)
                .subscribeAndLogErrors { selections ->
                    fragment.excludedGenres = enumSetOf(selections.map {
                        toSafeApiEnum(Genre::class.java, it)
                    })
                }

        val genreItems = Genre.values().map { getSafeApiEnum(it) }
        val languageItems = listOf(
                fragment.getString(R.string.fragment_media_list_all_languages),
                fragment.getString(R.string.language_german),
                fragment.getString(R.string.language_english)
        )

        fragment.languageSelector.items = languageItems
        fragment.genreSelector.items = genreItems
        fragment.excludedGenreSelector.items = genreItems
    }

    private fun measureTitle(): Int {
        val widthSpec = makeMeasureSpec(DeviceUtils.getScreenWidth(fragment.safeContext), View.MeasureSpec.EXACTLY)
        val heightSpec = makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        fragment.searchBottomSheetTitle.measure(widthSpec, heightSpec)

        return fragment.searchBottomSheetTitle.measuredHeight + fragment.dip(10)
    }

    private fun <T : Enum<T>> toSafeApiEnum(klass: Class<T>, value: String) = ProxerUtils.toApiEnum(klass, value)
            ?: throw IllegalArgumentException("Unknown ${klass.simpleName}: $value")

    private fun getSafeApiEnum(value: Enum<*>) = ProxerUtils.getApiEnumName(value)
            ?: throw IllegalArgumentException("Unknown ${value::class.java.simpleName}: ${value.name}")

    fun onBackPressed() = if (bottomSheetBehaviour.state != STATE_COLLAPSED) {
        bottomSheetBehaviour.state = STATE_COLLAPSED

        true
    } else {
        false
    }
}
