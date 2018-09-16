package me.proxer.app.ui.view

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.isGone
import com.jakewharton.rxbinding2.view.clicks
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.uber.autodispose.android.ViewScopeProvider
import com.uber.autodispose.kotlin.autoDisposable
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

    var items by Delegates.observable(listOf<String>()) { _, old, new ->
        if (old != new && isExtended) {
            itemContainer.removeAllViews()

            handleExtension()
        }
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

        resetButton.setIconicsImage(CommunityMaterial.Icon.cmd_undo, 32)
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
                notifySelectionChangedListener()
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
                    itemContainer.addView(
                        when (isSingleSelection) {
                            true -> constructSingleSelectionView(item)
                            false -> createMultiSelectionView(item)
                        }
                    )
                }
            }
        } else {
            itemContainer.removeAllViews()
        }

        if (isSingleSelection && children.none { it is RadioButton && it.isChecked }) {
            (children.firstOrNull() as? RadioButton)?.let {
                it.isChecked = true
                it.jumpDrawablesToCurrentState()
            }
        }
    }

    private fun handleSelection() {
        children.forEach {
            if (it is CheckBox) it.isChecked = selection.contains(it.text.toString())
        }
    }

    private fun constructSingleSelectionView(item: String): View {
        val radioButton = AppCompatRadioButton(context)

        radioButton.text = item
        radioButton.isChecked = selection.contains(item)

        radioButton.clicks()
            .autoDisposable(ViewScopeProvider.from(this))
            .subscribeAndLogErrors {
                selection.clear()
                selection.add(item)

                children.forEach { view ->
                    if (view is RadioButton && view != radioButton) view.isChecked = false
                }

                notifySelectionChangedListener()
            }

        return radioButton
    }

    private fun createMultiSelectionView(item: String): View {
        val checkBox = AppCompatCheckBox(context)

        checkBox.text = item
        checkBox.isChecked = selection.contains(item)

        checkBox.clicks()
            .autoDisposable(ViewScopeProvider.from(this))
            .subscribe {
                if (!selection.remove(item)) {
                    selection.add(item)
                }

                notifySelectionChangedListener()
            }

        return checkBox
    }

    private fun notifySelectionChangedListener() {
        selectionChangeSubject.onNext(selection)
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
