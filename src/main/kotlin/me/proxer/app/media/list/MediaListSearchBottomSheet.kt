package me.proxer.app.media.list

import android.support.design.widget.BottomSheetBehavior
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
import me.proxer.app.util.extension.getEnumSet
import me.proxer.app.util.extension.putEnumSet
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
        private const val GENRES_ARGUMENT = "genres"
        private const val EXCLUDED_GENRES_ARGUMENT = "excluded_genres"

        fun bindTo(fragment: MediaListFragment, viewModel: MediaListViewModel) {
            MediaListSearchBottomSheet(fragment, viewModel)
        }
    }

    private var genres: EnumSet<Genre>
        get() = fragment.safeArguments.getEnumSet(GENRES_ARGUMENT, Genre::class.java)
        set(value) {
            fragment.safeArguments.putEnumSet(GENRES_ARGUMENT, value)

            viewModel.genres = value
        }

    private var excludedGenres: EnumSet<Genre>
        get() = fragment.safeArguments.getEnumSet(EXCLUDED_GENRES_ARGUMENT, Genre::class.java)
        set(value) {
            fragment.safeArguments.putEnumSet(GENRES_ARGUMENT, value)

            viewModel.excludedGenres = value
        }

    private val bottomSheetBehaviour = BottomSheetBehavior.from(fragment.searchBottomSheet)

    init {
        bottomSheetBehaviour.isHideable = false
        bottomSheetBehaviour.peekHeight = measureTitle()
        bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED

        fragment.genresContainer.enableLayoutAnimationsSafely()
        fragment.excludedGenresContainer.enableLayoutAnimationsSafely()

        fragment.genresToggleIcon.setImageDrawable(IconicsDrawable(fragment.context)
                .icon(CommunityMaterial.Icon.cmd_chevron_down)
                .colorRes(R.color.icon)
                .sizeDp(32)
                .paddingDp(8))

        fragment.excludedGenresToggleIcon.setImageDrawable(IconicsDrawable(fragment.context)
                .icon(CommunityMaterial.Icon.cmd_chevron_down)
                .colorRes(R.color.icon)
                .sizeDp(32)
                .paddingDp(8))

        fragment.searchBottomSheetContent.clicks()
                .autoDispose(fragment)
                .subscribe {
                    bottomSheetBehaviour.state = when (bottomSheetBehaviour.state) {
                        BottomSheetBehavior.STATE_EXPANDED -> BottomSheetBehavior.STATE_COLLAPSED
                        else -> BottomSheetBehavior.STATE_EXPANDED
                    }
                }

        fragment.genresToggle.clicks()
                .autoDispose(fragment)
                .subscribe {
                    if (fragment.genresContainer.childCount <= 0) {
                        val checkBoxes = constructCheckBoxes(Genre::class.java, { genres.contains(it) }, {
                            genres = when {
                                it.isEmpty() -> EnumSet.noneOf(Genre::class.java)
                                else -> EnumSet.copyOf(it)
                            }
                        })

                        checkBoxes.forEach { fragment.genresContainer.addView(it) }
                        ViewCompat.animate(fragment.genresToggleIcon).rotation(180f)
                    } else {
                        fragment.genresContainer.removeAllViews()
                        ViewCompat.animate(fragment.genresToggleIcon).rotation(0f)
                    }
                }

        fragment.excludedGenresToggle.clicks()
                .autoDispose(fragment)
                .subscribe {
                    if (fragment.excludedGenresContainer.childCount <= 0) {
                        val checkBoxes = constructCheckBoxes(Genre::class.java, { excludedGenres.contains(it) }, {
                            excludedGenres = when {
                                it.isEmpty() -> EnumSet.noneOf(Genre::class.java)
                                else -> EnumSet.copyOf(it)
                            }
                        })

                        checkBoxes.forEach { fragment.excludedGenresContainer.addView(it) }
                        ViewCompat.animate(fragment.excludedGenresToggleIcon).rotation(180f)
                    } else {
                        fragment.excludedGenresContainer.removeAllViews()
                        ViewCompat.animate(fragment.excludedGenresToggleIcon).rotation(0f)
                    }
                }

        fragment.search.clicks()
                .autoDispose(fragment)
                .subscribe { viewModel.reload() }
    }

    private fun measureTitle(): Int {
        val widthSpec = makeMeasureSpec(DeviceUtils.getScreenWidth(fragment.safeContext), View.MeasureSpec.EXACTLY)
        val heightSpec = makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        fragment.searchBottomSheetTitle.measure(widthSpec, heightSpec)

        return fragment.searchBottomSheetTitle.measuredHeight + fragment.dip(24)
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
