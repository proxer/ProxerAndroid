package me.proxer.app.settings.theme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.jakewharton.rxbinding3.view.clicks
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.settings.theme.ThemeAdapter.ViewHolder
import me.proxer.app.util.extension.mapBindingAdapterPosition

/**
 * @author Ruben Gees
 */
class ThemeAdapter(currentThemeContainer: ThemeContainer) : BaseAdapter<Theme, ViewHolder>() {

    val selected: Theme
        get() = data[selectedIndex]

    private val selectedVariant: ThemeVariant
    private var selectedIndex: Int

    init {
        data = Theme.values().toList()
        selectedVariant = currentThemeContainer.variant
        selectedIndex = data.indexOfFirst { it == currentThemeContainer.theme }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_theme, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun swapDataAndNotifyWithDiffing(newData: List<Theme>) {
        throw UnsupportedOperationException()
    }

    inner class ViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal val themeButton by bindView<ImageButton>(R.id.themeButton)

        fun bind(item: Theme) {
            val drawable = TwoColorSelectableDrawable(
                item.primaryColor(themeButton.context),
                item.secondaryColor(themeButton.context),
                if (selectedIndex == bindingAdapterPosition) item.colorOnSecondary(themeButton.context) else null
            )

            themeButton.contentDescription = themeButton.context.getString(item.themeName)
            themeButton.setImageDrawable(drawable)

            themeButton.clicks()
                .mapBindingAdapterPosition({ bindingAdapterPosition }) { it }
                .autoDisposable(this)
                .subscribe {
                    val previous = selectedIndex

                    selectedIndex = it

                    notifyItemChanged(previous)
                    notifyItemChanged(it)
                }
        }
    }
}
