package com.proxerme.app.fragment.user

import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import butterknife.bindView
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.adapter.user.ToptenAdapter
import com.proxerme.app.fragment.framework.EasyLoadingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.user.entitiy.ToptenEntry
import com.proxerme.library.connection.user.request.ToptenRequest
import com.proxerme.library.parameters.CategoryParameter.ANIME
import com.proxerme.library.parameters.CategoryParameter.MANGA

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ToptenFragment : EasyLoadingFragment<Array<Array<ToptenEntry>>>() {

    companion object {
        private const val ARGUMENT_USER_ID = "user_id"
        private const val ARGUMENT_USER_NAME = "user_name"

        fun newInstance(userId: String? = null, userName: String? = null): ToptenFragment {
            if (userId.isNullOrBlank() && userName.isNullOrBlank()) {
                throw IllegalArgumentException("You must provide at least one of the arguments")
            }

            return ToptenFragment().apply {
                this.arguments = android.os.Bundle().apply {
                    this.putString(ToptenFragment.Companion.ARGUMENT_USER_ID, userId)
                    this.putString(ToptenFragment.Companion.ARGUMENT_USER_NAME, userName)
                }
            }
        }
    }

    override val section = Section.TOPTEN

    private var userId: String? = null
    private var userName: String? = null

    private lateinit var animeAdapter: ToptenAdapter
    private lateinit var mangaAdapter: ToptenAdapter

    private val animeContainer: ViewGroup by bindView(com.proxerme.app.R.id.animeContainer)
    private val mangaContainer: ViewGroup by bindView(com.proxerme.app.R.id.mangaContainer)
    private val animeList: RecyclerView by bindView(com.proxerme.app.R.id.animeList)
    private val mangaList: RecyclerView by bindView(com.proxerme.app.R.id.mangaList)

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        userId = arguments.getString(ToptenFragment.Companion.ARGUMENT_USER_ID)
        userName = arguments.getString(ToptenFragment.Companion.ARGUMENT_USER_NAME)

        animeAdapter = ToptenAdapter(savedInstanceState, ANIME)
        animeAdapter.callback = object : ToptenAdapter.ToptenAdapterCallback() {
            override fun onItemClick(v: View, item: ToptenEntry) {
                MediaActivity.navigateTo(activity, item.id, item.name)
            }
        }

        mangaAdapter = ToptenAdapter(savedInstanceState, MANGA)
        mangaAdapter.callback = object : ToptenAdapter.ToptenAdapterCallback() {
            override fun onItemClick(v: View, item: ToptenEntry) {
                MediaActivity.navigateTo(activity, item.id, item.name)
            }
        }
    }

    override fun onDestroy() {
        animeAdapter.callback = null
        mangaAdapter.callback = null

        super.onDestroy()
    }

    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?,
                              savedInstanceState: android.os.Bundle?): android.view.View {
        return inflater.inflate(com.proxerme.app.R.layout.fragment_topten, container, false)
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        animeList.isNestedScrollingEnabled = false
        animeList.layoutManager = GridLayoutManager(context, Utils.calculateSpanAmount(activity) + 1)
        animeList.adapter = animeAdapter
        mangaList.isNestedScrollingEnabled = false
        mangaList.layoutManager = GridLayoutManager(context, Utils.calculateSpanAmount(activity) + 1)
        mangaList.adapter = mangaAdapter

        show()
    }

    override fun onSaveInstanceState(outState: android.os.Bundle) {
        super.onSaveInstanceState(outState)

        animeAdapter.saveInstanceState(outState)
        mangaAdapter.saveInstanceState(outState)
    }

    override fun constructLoadingRequest(): LoadingRequest<Array<Array<ToptenEntry>>> {
        return LoadingRequest(ToptenRequest(userId, userName, ANIME),
                ToptenRequest(userId, userName, MANGA), zipFunction = { partialResults ->

            @Suppress("UNCHECKED_CAST")
            (com.proxerme.app.fragment.framework.RetainedLoadingFragment.LoadingResult(arrayOf(
                    partialResults[0] as Array<ToptenEntry>,
                    partialResults[1] as Array<ToptenEntry>
            )))
        })
    }

    override fun clear() {
        animeAdapter.clear()
        mangaAdapter.clear()
    }

    override fun save(result: Array<Array<ToptenEntry>>) {
        animeAdapter.replace(result[0])
        mangaAdapter.replace(result[1])
    }

    override fun show() {
        if (animeAdapter.isEmpty()) {
            animeContainer.visibility = View.GONE
        } else {
            animeContainer.visibility = View.VISIBLE
        }

        if (mangaAdapter.isEmpty()) {
            mangaContainer.visibility = View.GONE
        } else {
            mangaContainer.visibility = View.VISIBLE
        }
    }
}