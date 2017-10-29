package me.proxer.app.media.list

import android.support.design.widget.BottomSheetBehavior.STATE_COLLAPSED
import android.support.design.widget.BottomSheetBehavior.STATE_EXPANDED
import android.support.design.widget.BottomSheetBehavior.from
import android.support.v4.view.ViewCompat
import android.support.v7.widget.AppCompatCheckBox
import android.view.View
import android.view.View.MeasureSpec.makeMeasureSpec
import android.view.ViewGroup
import android.widget.CheckBox
import com.jakewharton.rxbinding2.view.clicks
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.dip
import me.proxer.app.util.extension.enableLayoutAnimationsSafely
import me.proxer.library.enums.Genre
import me.proxer.library.util.ProxerUtils
import org.jetbrains.anko.childrenSequence
import java.util.EnumSet

/**
 * @author Ruben Gees
 */
class MediaListSearchBottomSheet private constructor(
        private val fragment: MediaListFragment,
        private val viewModel: MediaListViewModel
) {

    companion object {
        private const val GENRES_EXPANDED_ARGUMENT = "are_genres_expanded"
        private const val EXCLUDED_GENRES_EXPANDED_ARGUMENT = "are_excluded_genres_expanded"

        fun bindTo(fragment: MediaListFragment, viewModel: MediaListViewModel) {
            MediaListSearchBottomSheet(fragment, viewModel)
        }
    }

    private var areGenresExpanded
        get() = fragment.safeArguments.getBoolean(GENRES_EXPANDED_ARGUMENT, false)
        set(value) {
            fragment.safeArguments.putBoolean(GENRES_EXPANDED_ARGUMENT, value)
        }

    private var areExcludedGenresExpanded
        get() = fragment.safeArguments.getBoolean(EXCLUDED_GENRES_EXPANDED_ARGUMENT, false)
        set(value) {
            fragment.safeArguments.putBoolean(EXCLUDED_GENRES_EXPANDED_ARGUMENT, value)
        }

    private val bottomSheetBehaviour = from(fragment.searchBottomSheet)

    init {
        bottomSheetBehaviour.isHideable = false
        bottomSheetBehaviour.peekHeight = measureTitle()
        bottomSheetBehaviour.state = STATE_COLLAPSED

        fragment.genresContainer.enableLayoutAnimationsSafely()
        fragment.excludedGenresContainer.enableLayoutAnimationsSafely()

        fragment.genresToggleButton.setImageDrawable(IconicsDrawable(fragment.context)
                .icon(CommunityMaterial.Icon.cmd_chevron_down)
                .colorRes(R.color.icon)
                .sizeDp(32)
                .paddingDp(8))

        fragment.excludedGenresToggleButton.setImageDrawable(IconicsDrawable(fragment.context)
                .icon(CommunityMaterial.Icon.cmd_chevron_down)
                .colorRes(R.color.icon)
                .sizeDp(32)
                .paddingDp(8))

        fragment.genresResetIcon.setImageDrawable(IconicsDrawable(fragment.context)
                .icon(CommunityMaterial.Icon.cmd_undo)
                .colorRes(R.color.icon)
                .sizeDp(32)
                .paddingDp(8))

        fragment.excludedGenresResetIcon.setImageDrawable(IconicsDrawable(fragment.context)
                .icon(CommunityMaterial.Icon.cmd_undo)
                .colorRes(R.color.icon)
                .sizeDp(32)
                .paddingDp(8))

        fragment.searchBottomSheetTitle.clicks()
                .autoDispose(fragment)
                .subscribe {
                    bottomSheetBehaviour.state = when (bottomSheetBehaviour.state) {
                        STATE_EXPANDED -> STATE_COLLAPSED
                        else -> STATE_EXPANDED
                    }
                }

        fragment.genresToggle.clicks().mergeWith(fragment.genresToggleButton.clicks())
                .autoDispose(fragment)
                .subscribe { handleGenreExpansion(fragment.genresContainer.childCount <= 0) }

        fragment.excludedGenresToggle.clicks().mergeWith(fragment.excludedGenresToggleButton.clicks())
                .autoDispose(fragment)
                .subscribe { handleExcludedGenreExpansion(fragment.excludedGenresContainer.childCount <= 0) }

        fragment.genresResetIcon.clicks()
                .autoDispose(fragment)
                .subscribe {
                    fragment.genres = EnumSet.noneOf(Genre::class.java)

                    fragment.genresContainer.childrenSequence().forEach {
                        if (it is CheckBox) it.isChecked = false
                    }
                }

        fragment.excludedGenresResetIcon.clicks()
                .autoDispose(fragment)
                .subscribe {
                    fragment.excludedGenres = EnumSet.noneOf(Genre::class.java)

                    fragment.excludedGenresContainer.childrenSequence().forEach {
                        if (it is CheckBox) it.isChecked = false
                    }
                }

        fragment.search.clicks()
                .autoDispose(fragment)
                .subscribe {
                    bottomSheetBehaviour.state = STATE_COLLAPSED

                    viewModel.reload()
                }

        handleGenreExpansion(areGenresExpanded)
        handleExcludedGenreExpansion(areExcludedGenresExpanded)
    }

    private fun measureTitle(): Int {
        val widthSpec = makeMeasureSpec(DeviceUtils.getScreenWidth(fragment.safeContext), View.MeasureSpec.EXACTLY)
        val heightSpec = makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        fragment.searchBottomSheetTitle.measure(widthSpec, heightSpec)

        return fragment.searchBottomSheetTitle.measuredHeight + fragment.dip(16)
    }

    private fun handleGenreExpansion(expand: Boolean) {
        areGenresExpanded = expand

        fragment.genresContainer.removeAllViews()
        ViewCompat.animate(fragment.genresToggleButton).rotation(if (expand) 180f else 0f)

        if (expand) {
            val genres = fragment.genres
            val checkBoxes = constructCheckBoxes(Genre::class.java, { genres.contains(it) }, {
                fragment.genres = when {
                    it.isEmpty() -> EnumSet.noneOf(Genre::class.java)
                    else -> EnumSet.copyOf(it)
                }
            })

            checkBoxes.forEach { fragment.genresContainer.addView(it) }
        }
    }

    private fun handleExcludedGenreExpansion(expand: Boolean) {
        areExcludedGenresExpanded = expand

        fragment.excludedGenresContainer.removeAllViews()
        ViewCompat.animate(fragment.excludedGenresToggleButton).rotation(if (expand) 180f else 0f)

        if (expand) {
            val excludedGenres = fragment.excludedGenres
            val checkBoxes = constructCheckBoxes(Genre::class.java, { excludedGenres.contains(it) }, {
                fragment.excludedGenres = when {
                    it.isEmpty() -> EnumSet.noneOf(Genre::class.java)
                    else -> EnumSet.copyOf(it)
                }
            })

            checkBoxes.forEach { fragment.excludedGenresContainer.addView(it) }
        }
    }

    private fun <T : Enum<T>> constructCheckBoxes(
            klass: Class<T>,
            shouldCheck: (T) -> Boolean,
            onClick: (selection: List<T>) -> Unit
    ) = klass.enumConstants.map {
        val checkBox = AppCompatCheckBox(fragment.context)

        checkBox.text = ProxerUtils.getApiEnumName(it)
        checkBox.isChecked = shouldCheck(it)
        checkBox.clicks()
                .autoDispose(fragment)
                .subscribe {
                    val selection = (checkBox.parent as ViewGroup).childrenSequence().map {
                        val isChecked = it is CheckBox && it.isChecked

                        when {
                            isChecked -> ProxerUtils.toApiEnum(klass, (it as CheckBox).text.toString())
                            else -> null
                        }
                    }

                    onClick(selection.filterNotNull().toList())
                }

        checkBox
    }
}
