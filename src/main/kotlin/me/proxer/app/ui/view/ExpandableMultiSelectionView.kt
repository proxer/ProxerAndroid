package me.proxer.app.ui.view

import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.support.v4.view.ViewCompat
import android.support.v7.widget.AppCompatCheckBox
import android.util.AttributeSet
import android.util.SparseArray
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.jakewharton.rxbinding2.view.clicks
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.subscribeAndLogErrors
import org.jetbrains.anko.childrenSequence
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ExpandableMultiSelectionView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val controlContainer by bindView<ViewGroup>(R.id.controlContainer)
    private val title by bindView<TextView>(R.id.title)
    private val resetButton by bindView<ImageButton>(R.id.resetButton)
    private val toggleButton by bindView<ImageButton>(R.id.toggleButton)
    private val itemContainer by bindView<ViewGroup>(R.id.items)

    val selectionChangeSubject: PublishSubject<Map<Int, String>> = PublishSubject.create()

    var titleText: CharSequence
        get() = title.text
        set(value) {
            title.text = value
        }

    var items by Delegates.observable(emptyList<String>(), { _, old, new ->
        if (old != new && isExtended) {
            itemContainer.removeAllViews()

            handleExtension()
        }
    })

    var selection by Delegates.observable(ParcelableStringBooleanMap(), { _, old, new ->
        if (old != new) handleSelection()
    })

    var isExtended by Delegates.observable(false, { _, old, new ->
        if (old != new) handleExtension()
    })

    init {
        orientation = VERTICAL

        inflate(context, R.layout.view_expandable_multi_selection, this)

        context.theme.obtainStyledAttributes(attrs, R.styleable.ExpandableMultiSelectionView, 0, 0).apply {
            titleText = getString(R.styleable.ExpandableMultiSelectionView_titleText)

            recycle()
        }

        resetButton.setIconicsImage(CommunityMaterial.Icon.cmd_undo, 32)
        toggleButton.setIconicsImage(CommunityMaterial.Icon.cmd_chevron_down, 32)

        controlContainer.clicks().mergeWith(toggleButton.clicks())
                .autoDispose(context as LifecycleOwner)
                .subscribeAndLogErrors {
                    isExtended = !isExtended
                }

        resetButton.clicks()
                .autoDispose(context as LifecycleOwner)
                .subscribeAndLogErrors {
                    itemContainer.childrenSequence().forEach {
                        if (it is CheckBox) it.isChecked = false

                        selection.clear()

                        handleSelection()
                        notifySelectionChangedListener()
                    }
                }
    }

    override fun onSaveInstanceState(): Parcelable = SavedState(super.onSaveInstanceState(), selection, isExtended)

    override fun onRestoreInstanceState(state: Parcelable) {
        state as SavedState

        super.onRestoreInstanceState(state.superState)

        selection = state.selection
        isExtended = state.isExtended

        handleExtension()
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchFreezeSelfOnly(container)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchThawSelfOnly(container)
    }

    private fun handleExtension() {
        ViewCompat.animate(toggleButton).rotation(if (isExtended) 180f else 0f)

        if (isExtended) {
            if (itemContainer.childCount <= 0) {
                items.forEach { item ->
                    val checkBox = AppCompatCheckBox(context)

                    checkBox.text = item
                    checkBox.isChecked = selection[item] == true
                    checkBox.clicks()
                            .autoDispose(context as LifecycleOwner)
                            .subscribeAndLogErrors {
                                selection.putOrRemove(item)

                                notifySelectionChangedListener()
                            }

                    itemContainer.addView(checkBox)
                }
            }
        } else {
            itemContainer.removeAllViews()
        }
    }

    private fun handleSelection() {
        childrenSequence().forEach {
            if (it is CheckBox) it.isChecked = selection[it.text.toString()] == true
        }
    }

    private fun notifySelectionChangedListener() {
        val selectionMap = selection.keys.mapIndexed { index, item -> index to item }.toMap()

        selectionChangeSubject.onNext(selectionMap)
    }

    internal class SavedState : BaseSavedState {
        companion object {
            @Suppress("unused")
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel) = SavedState(source)
                override fun newArray(size: Int) = arrayOfNulls<SavedState?>(size)
            }
        }

        internal val selection: ParcelableStringBooleanMap
        internal val isExtended: Boolean

        internal constructor(
                superState: Parcelable,
                selection: ParcelableStringBooleanMap,
                isExtended: Boolean
        ) : super(superState) {
            this.selection = selection
            this.isExtended = isExtended
        }

        internal constructor(state: Parcel) : super(state) {
            selection = ParcelableStringBooleanMap.CREATOR.createFromParcel(state)
            isExtended = state.readInt() == 1
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)

            selection.writeToParcel(out, 0)
            out.writeInt(if (isExtended) 1 else 0)
        }
    }
}
