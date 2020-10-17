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
import androidx.core.view.updateLayoutParams
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.uber.autodispose.android.ViewScopeProvider
import com.uber.autodispose.autoDisposable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.util.extension.dip
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
        if (old != new) {
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
    private var inflationDisposable: Disposable? = null

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

        resetButton.setIconicsImage(CommunityMaterial.Icon3.cmd_undo, 32)
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
    }

    override fun onDetachedFromWindow() {
        inflationDisposable?.dispose()
        inflationDisposable = null

        super.onDetachedFromWindow()
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

        inflationDisposable?.dispose()
        itemContainer.removeAllViews()

        if (isExtended) {
            inflationDisposable = Flowable.fromIterable(items)
                .concatMapEager { item ->
                    val inflationSingle = when {
                        isSingleSelection -> createSingleSelectionView(item)
                        else -> createMultiSelectionView(item)
                    }

                    inflationSingle
                        .toFlowable()
                        .map { view -> view to item }
                        .subscribeOn(Schedulers.computation())
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterNext { (view, item) ->
                    when (view) {
                        is RadioButton -> initSingleSelectionListener(view, item.value)
                        is CheckBox -> initMultiSelectionListener(view, item.value)
                    }
                }
                .doOnComplete {
                    if (isSingleSelection) {
                        val radioButtonChildren = itemContainer.children.filterIsInstance(RadioButton::class.java)
                        val isNoneChecked = radioButtonChildren.none { it.isChecked }

                        if (isNoneChecked) {
                            radioButtonChildren.firstOrNull()?.isChecked = true
                        }

                        jumpDrawablesToCurrentState()
                    }
                }
                .subscribeAndLogErrors { (view, _) -> itemContainer.addView(view) }
        }
    }

    private fun handleSelection() {
        if (inflationDisposable?.isDisposed != false) {
            itemContainer.children
                .filterIsInstance(CheckBox::class.java)
                .forEach { it.isChecked = selection.contains(it.text.toString()) }
        }
    }

    private fun createSingleSelectionView(item: Item): Single<View> = Single.fromCallable {
        val radioButton = LayoutInflater.from(context)
            .inflate(R.layout.item_radio_button, this, false) as RadioButton

        radioButton.text = item.value
        radioButton.isChecked = selection.contains(item.value)

        radioButton.updateLayoutParams<MarginLayoutParams> {
            marginStart = -context.dip(5)
            marginEnd = context.dip(5)
        }

        TooltipCompat.setTooltipText(radioButton, item.description)

        radioButton.clicks()
            .autoDisposable(ViewScopeProvider.from(this))
            .subscribeAndLogErrors {
                selection.clear()
                selection.add(item.value)

                itemContainer.children
                    .filterIsInstance(RadioButton::class.java)
                    .forEach { if (it != radioButton) it.isChecked = false }

                selectionChangeSubject.onNext(selection)
            }

        radioButton
    }

    private fun createMultiSelectionView(item: Item): Single<View> = Single.fromCallable {
        val checkBox = LayoutInflater.from(context)
            .inflate(R.layout.item_checkbox, this, false) as CheckBox

        checkBox.text = item.value
        checkBox.isChecked = selection.contains(item.value)

        checkBox.updateLayoutParams<MarginLayoutParams> {
            marginStart = -context.dip(5)
            marginEnd = context.dip(5)
        }

        TooltipCompat.setTooltipText(checkBox, item.description)

        checkBox
    }

    private fun initSingleSelectionListener(view: RadioButton, item: String) {
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

    private fun initMultiSelectionListener(view: CheckBox, item: String) {
        view.clicks()
            .autoDisposable(ViewScopeProvider.from(this))
            .subscribeAndLogErrors {
                if (!selection.remove(item)) {
                    selection.add(item)
                }

                selectionChangeSubject.onNext(selection)
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
