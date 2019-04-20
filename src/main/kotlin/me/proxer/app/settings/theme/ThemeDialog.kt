package me.proxer.app.settings.theme

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseDialog
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ThemeDialog : BaseDialog() {

    companion object {
        fun show(activity: AppCompatActivity) = ThemeDialog()
            .show(activity.supportFragmentManager, "theme_dialog")
    }

    private val colorList by bindView<RecyclerView>(R.id.colorList)
    private val nightSwitch by bindView<SwitchCompat>(R.id.nightSwitch)

    private var adapter by Delegates.notNull<ThemeAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ThemeAdapter(preferenceHelper.themeContainer)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog(requireContext())
        .noAutoDismiss()
        .title(R.string.dialog_theme_title)
        .positiveButton(R.string.dialog_theme_positive) {
            val theme = adapter.selected

            val variant = when (nightSwitch.isChecked) {
                true -> ThemeVariant.DARK
                false -> ThemeVariant.LIGHT
            }

            preferenceHelper.themeContainer = ThemeContainer(theme, variant)

            dismiss()
        }
        .negativeButton(R.string.cancel) { it.dismiss() }
        .customView(R.layout.dialog_theme)

    override fun onDialogCreated(savedInstanceState: Bundle?) {
        super.onDialogCreated(savedInstanceState)

        colorList.isNestedScrollingEnabled = false
        colorList.layoutManager = GridLayoutManager(colorList.context, 4)
        colorList.adapter = adapter

        nightSwitch.isChecked = preferenceHelper.themeContainer.variant == ThemeVariant.DARK
    }
}
