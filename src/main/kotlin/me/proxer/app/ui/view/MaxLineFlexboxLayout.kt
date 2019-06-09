package me.proxer.app.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.View.MeasureSpec.makeMeasureSpec
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.withStyledAttributes
import com.google.android.flexbox.FlexboxLayout
import com.jakewharton.rxbinding3.view.clicks
import com.uber.autodispose.android.ViewScopeProvider
import com.uber.autodispose.autoDisposable
import io.reactivex.subjects.PublishSubject
import me.proxer.app.R
import me.proxer.app.util.extension.resolveColor

/**
 * @author Ruben Gees
 */
class MaxLineFlexboxLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FlexboxLayout(context, attrs, defStyleAttr) {

    val showAllEvents: PublishSubject<Unit> = PublishSubject.create()

    var maxLines = Int.MAX_VALUE

    private var currentWidth: Int = 0
    private var currentLine: Int = 1
    private var isShowAllButtonEnabled = false

    init {
        if (attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.MaxLineFlexboxLayout) {
                maxLines = getInt(R.styleable.MaxLineFlexboxLayout_maxLines, Int.MAX_VALUE)
            }
        }
    }

    override fun removeAllViews() {
        isShowAllButtonEnabled = false
        currentWidth = 0
        currentLine = 1

        super.removeAllViews()
    }

    override fun addView(child: View) {
        if (canAddView(child)) {
            if (currentWidth + child.measuredWidth > width) {
                currentWidth = child.measuredWidth
                currentLine++
            } else {
                currentWidth += child.measuredWidth
            }

            super.addView(child)
        }
    }

    fun canAddView(view: View): Boolean {
        require(width != 0) { "Only call this method after this view has been laid out." }

        if (view.measuredWidth == 0) {
            view.measure(makeMeasureSpec(width, AT_MOST), makeMeasureSpec(0, UNSPECIFIED))
        }

        return currentWidth + view.measuredWidth <= width || currentLine + 1 <= maxLines
    }

    fun enableShowAllButton() {
        if (isShowAllButtonEnabled) return

        val container = FrameLayout(context)
        val button = AppCompatButton(context, null, android.R.attr.borderlessButtonStyle)

        button.text = context.getString(R.string.fragment_media_info_show_all)
        button.layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER)
        button.setTextColor(context.resolveColor(R.attr.colorSecondary))

        button.clicks()
            .doOnNext { maxLines = Int.MAX_VALUE }
            .autoDisposable(ViewScopeProvider.from(this))
            .subscribe(showAllEvents)

        container.layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            isWrapBefore = true
        }

        container.addView(button)
        super.addView(container)
    }
}
