package me.proxer.app.fragment.manga

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.MangaActivity
import me.proxer.app.activity.MediaActivity
import me.proxer.app.adapter.manga.LocalMangaAdapter
import me.proxer.app.entity.LocalMangaChapter
import me.proxer.app.fragment.base.LoadingFragment
import me.proxer.app.task.manga.LocalMangaListTask
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.CompleteLocalMangaEntry
import me.proxer.app.util.extension.bindView
import me.proxer.library.entitiy.info.EntryCore
import me.proxer.library.enums.Category
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.find

/**
 * @author Ruben Gees
 */
class LocalMangaFragment : LoadingFragment<Unit, List<CompleteLocalMangaEntry>>() {

    companion object {
        fun newInstance(): LocalMangaFragment {
            return LocalMangaFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    override val isLoginRequired = true

    private lateinit var adapter: LocalMangaAdapter

    private val list: RecyclerView by bindView(R.id.list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = LocalMangaAdapter(savedInstanceState)
        adapter.callback = object : LocalMangaAdapter.LocalMangaAdapterCallback {
            override fun onChapterClick(entry: EntryCore, chapter: LocalMangaChapter) {
                MangaActivity.navigateTo(activity, entry.id, chapter.episode, chapter.language, chapter.title,
                        entry.name, entry.episodeAmount)
            }

            override fun onChapterLongClick(view: View, entry: EntryCore) {
                val imageView = view.find<ImageView>(R.id.image)

                MediaActivity.navigateTo(activity, entry.id, entry.name, Category.MANGA,
                        if (imageView.drawable != null) imageView else null)
            }

            override fun onDeleteClick(entry: EntryCore, chapter: LocalMangaChapter) {
                // TODO
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_local_manga, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.setHasFixedSize(true)
        list.layoutManager = StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity) + 1,
                StaggeredGridLayoutManager.VERTICAL)
        list.adapter = adapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }

    override fun onSuccess(result: List<CompleteLocalMangaEntry>) {
        adapter.replace(result)

        super.onSuccess(result)
    }

    override fun showContent() {
        super.showContent()

        if (adapter.isEmpty()) {
            showError(R.string.error_no_data_local_manga, ErrorUtils.ErrorAction.ACTION_MESSAGE_HIDE)
        }
    }

    override fun constructInput() = Unit
    override fun constructTask() = TaskBuilder.task(LocalMangaListTask())
            .async()
            .build()
}