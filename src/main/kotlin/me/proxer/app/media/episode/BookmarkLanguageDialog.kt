package me.proxer.app.media.episode

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.customListAdapter
import com.jakewharton.rxbinding3.view.clicks
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseDialog
import me.proxer.app.util.extension.mapBindingAdapterPosition
import me.proxer.app.util.extension.toAppDrawable
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.enums.MediaLanguage
import me.proxer.library.util.ProxerUtils

class BookmarkLanguageDialog : BaseDialog() {

    companion object {
        const val LANGUAGE_RESULT = "language"

        private const val LANGUAGES_ARGUMENT = "languages"

        fun show(activity: FragmentActivity, languages: Set<MediaLanguage>) {
            val stringLanguages = languages.map { ProxerUtils.getSafeApiEnumName(it) }

            BookmarkLanguageDialog()
                .apply { arguments = bundleOf(LANGUAGES_ARGUMENT to ArrayList(stringLanguages)) }
                .show(activity.supportFragmentManager, "no_wifi_dialog")
        }
    }

    private val languages: Set<MediaLanguage>
        get() = requireNotNull(requireArguments().getStringArrayList(LANGUAGES_ARGUMENT))
            .map { ProxerUtils.toSafeApiEnum<MediaLanguage>(it) }
            .toSet()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val adapter = LanguageAdapter(languages.toList())

        adapter.clickSubject
            .autoDisposable(dialogLifecycleOwner.scope())
            .subscribe {
                setFragmentResult(LANGUAGE_RESULT, bundleOf(LANGUAGE_RESULT to ProxerUtils.getSafeApiEnumName(it)))

                dismiss()
            }

        return MaterialDialog(requireContext())
            .title(res = R.string.fragment_episodes_bookmark_language_dialog_title)
            .customListAdapter(adapter)
    }

    private class LanguageAdapter(
        private val languages: List<MediaLanguage>
    ) : RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {

        val clickSubject: PublishSubject<MediaLanguage> = PublishSubject.create()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_bookmark_language, parent, false)
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(languages[position])
        }

        override fun getItemCount(): Int {
            return languages.size
        }

        inner class ViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

            internal val container: ViewGroup by bindView(R.id.container)
            internal val image: ImageView by bindView(R.id.image)
            internal val text: TextView by bindView(R.id.text)

            fun bind(language: MediaLanguage) {
                container.clicks()
                    .mapBindingAdapterPosition({ bindingAdapterPosition }) { languages[it] }
                    .autoDisposable(this)
                    .subscribe(clickSubject)

                image.setImageDrawable(language.toGeneralLanguage().toAppDrawable(image.context))
                text.text = language.toAppString(text.context)
            }
        }
    }
}
