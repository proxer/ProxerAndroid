package com.proxerme.app.fragment.manga

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import butterknife.bindView
import com.proxerme.app.R
import com.proxerme.app.adapter.manga.MangaAdapter
import com.proxerme.app.fragment.framework.EasyLoadingFragment
import com.proxerme.app.manager.SectionManager
import com.proxerme.library.connection.manga.entity.Chapter
import com.proxerme.library.connection.manga.request.ChapterRequest

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MangaFragment : EasyLoadingFragment<Chapter>() {

    companion object {
        private const val ARGUMENT_ID = "id"
        private const val ARGUMENT_EPISODE = "episode"
        private const val ARGUMENT_LANGUAGE = "language"

        fun newInstance(id: String, episode: Int, language: String): MangaFragment {
            return MangaFragment().apply {
                this.arguments = Bundle().apply {
                    this.putString(ARGUMENT_ID, id)
                    this.putInt(ARGUMENT_EPISODE, episode)
                    this.putString(ARGUMENT_LANGUAGE, language)
                }
            }
        }
    }

    override val section = SectionManager.Section.MANGA

    private val id: String
        get() = arguments.getString(ARGUMENT_ID)
    private val episode: Int
        get() = arguments.getInt(ARGUMENT_EPISODE)
    private val language: String
        get() = arguments.getString(ARGUMENT_LANGUAGE)

    private lateinit var adapter: MangaAdapter

    private val pages: RecyclerView by bindView(R.id.pages)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = MangaAdapter(savedInstanceState)
    }

    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?,
                              savedInstanceState: android.os.Bundle?): android.view.View {
        return inflater.inflate(R.layout.fragment_manga, container, false)
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pages.layoutManager = LinearLayoutManager(context)
        pages.adapter = adapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }

    override fun save(result: Chapter) {
        adapter.init(result.server, result.entryId, result.id)
        adapter.replace(result.pages)
    }

    override fun show() {
        // Nothing to do
    }

    override fun clear() {
        adapter.clear()
    }

    override fun constructLoadingRequest(): LoadingRequest<Chapter> {
        return LoadingRequest(ChapterRequest(id, episode, language))
    }
}