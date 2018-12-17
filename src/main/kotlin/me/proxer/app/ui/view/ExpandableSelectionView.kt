package me.proxer.app.ui.view

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.isGone
import com.jakewharton.rxbinding2.view.clicks
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.uber.autodispose.android.ViewScopeProvider
import com.uber.autodispose.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.subscribeAndLogErrors
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ExpandableSelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val controlContainer by bindView<ViewGroup>(R.id.controlContainer)
    private val title by bindView<TextView>(R.id.title)
    private val resetButton by bindView<ImageButton>(R.id.resetButton)
    private val toggleButton by bindView<ImageButton>(R.id.toggleButton)
    private val itemContainer by bindView<ViewGroup>(R.id.items)

    val selectionChangeSubject: PublishSubject<List<String>> = PublishSubject.create()

    var titleText: CharSequence
        get() = title.text
        set(value) {
            title.text = value
        }

    var items by Delegates.observable(emptyList<Item>()) { _, old, new ->
        if (old != new && isExtended) {
            itemContainer.removeAllViews()

            handleExtension()
        }
    }

    var simpleItems
        get() = items.map { (value, _) -> value }
        set(value) {
            items = value.map { Item(it, null) }
        }

    var selection by Delegates.observable(mutableListOf<String>()) { _, old, new ->
        if (old != new) handleSelection()
    }

    var isExtended by Delegates.observable(false) { _, old, new ->
        if (old != new) handleExtension()
    }

    private var isSingleSelection = false

    init {
        orientation = VERTICAL

        inflate(context, R.layout.view_expandable_multi_selection, this)

        context.theme.obtainStyledAttributes(attrs, R.styleable.ExpandableSelectionView, 0, 0).apply {
            titleText = getString(R.styleable.ExpandableSelectionView_titleText) ?: ""
            isSingleSelection = getBoolean(R.styleable.ExpandableSelectionView_singleSelection, false)

            recycle()
        }

        if (isSingleSelection) {
            resetButton.isGone = true
        }

        resetButton.setIconicsImage(CommunityMaterial.Icon2.cmd_undo, 32)
        toggleButton.setIconicsImage(CommunityMaterial.Icon.cmd_chevron_down, 32)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        controlContainer.clicks().mergeWith(toggleButton.clicks())
            .autoDisposable(ViewScopeProvider.from(this))
            .subscribe { isExtended = !isExtended }

        resetButton.clicks()
            .autoDisposable(ViewScopeProvider.from(this))
            .subscribe {
                selection = mutableListOf()

                handleSelection()

                selectionChangeSubject.onNext(selection)
            }

        if (isSingleSelection) {
            initSingleSelectionListeners()
        } else {
            initMultiSelectionListeners()
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
                items
                    .map { if (isSingleSelection) createSingleSelectionView(it) else createMultiSelectionView(it) }
                    .forEach { itemContainer.addView(it) }

                if (ViewCompat.isAttachedToWindow(this)) {
                    if (isSingleSelection) initSingleSelectionListeners() else initMultiSelectionListeners()
                }
            }
        } else {
            itemContainer.removeAllViews()
        }

        if (isSingleSelection) {
            itemContainer.children
                .filterIsInstance(RadioButton::class.java)
                .filter { it.isChecked.not() }
                .firstOrNull()
                ?.apply {
                    isChecked = true
                    jumpDrawablesToCurrentState()
                }
        }
    }

    private fun handleSelection() {
        itemContainer.children
            .filterIsInstance(CheckBox::class.java)
            .forEach { it.isChecked = selection.contains(it.text.toString()) }
    }

    private fun createSingleSelectionView(item: Item): View {
        val radioButton = LayoutInflater.from(context)
            .inflate(R.layout.item_radio_button, this, false) as RadioButton

        radioButton.text = item.value
        radioButton.isChecked = selection.contains(item.value)

        TooltipCompat.setTooltipText(radioButton, item.description)

        return radioButton
    }

    private fun createMultiSelectionView(item: Item): View {
        val checkBox = LayoutInflater.from(context)
            .inflate(R.layout.item_checkbox, this, false) as CheckBox

        checkBox.text = item.value
        checkBox.isChecked = selection.contains(item.value)

        TooltipCompat.setTooltipText(checkBox, item.description)

        return checkBox
    }

    private fun initSingleSelectionListeners() {
        simpleItems.zip(itemContainer.children.toList()).forEach { (item, view) ->
            view.clicks()
                .autoDisposable(ViewScopeProvider.from(this))
                .subscribeAndLogErrors {
                    selection.clear()
                    selection.add(item)

                    itemContainer.children
                        .filterIsInstance(RadioButton::class.java)
                        .forEach { if (it != view) it.isChecked = false }

                    selectionChangeSubject.onNext(selection)
                }
        }
    }

    private fun initMultiSelectionListeners() {
        simpleItems.zip(itemContainer.children.toList()).forEach { (item, view) ->
            view.clicks()
                .autoDisposable(ViewScopeProvider.from(this))
                .subscribeAndLogErrors {
                    if (!selection.remove(item)) {
                        selection.add(item)
                    }

                    selectionChangeSubject.onNext(selection)
                }
        }
    }

    data class Item(val value: String, val description: String?)

    internal class SavedState : BaseSavedState {

        companion object {

            @Suppress("unused")
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel) = SavedState(source)
                override fun newArray(size: Int) = arrayOfNulls<SavedState?>(size)
            }
        }

        internal val selection: MutableList<String>
        internal val isExtended: Boolean

        internal constructor(
            superState: Parcelable?,
            selection: MutableList<String>,
            isExtended: Boolean
        ) : super(superState) {
            this.selection = selection
            this.isExtended = isExtended
        }

        internal constructor(state: Parcel) : super(state) {
            selection = mutableListOf<String>().also { state.readStringList(it) }
            isExtended = state.readInt() == 1
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)

            out.writeStringList(selection)
            out.writeInt(if (isExtended) 1 else 0)
        }
    }
}
