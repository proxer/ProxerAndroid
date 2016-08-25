package com.proxerme.app.fragment

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.bindView
import com.proxerme.app.R
import com.proxerme.app.adapter.ToptenAdapter
import com.proxerme.app.fragment.framework.EasyLoadingFragment
import com.proxerme.app.fragment.framework.RetainedLoadingFragment
import com.proxerme.app.manager.SectionManager
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
                this.arguments = Bundle().apply {
                    this.putString(ARGUMENT_USER_ID, userId)
                    this.putString(ARGUMENT_USER_NAME, userName)
                }
            }
        }
    }

    override val section = SectionManager.Section.TOPTEN

    private var userId: String? = null
    private var userName: String? = null

    private lateinit var animeAdapter: ToptenAdapter
    private lateinit var mangaAdapter: ToptenAdapter

    private val animeContainer: ViewGroup by bindView(R.id.animeContainer)
    private val mangaContainer: ViewGroup by bindView(R.id.mangaContainer)
    private val animeList: RecyclerView by bindView(R.id.animeList)
    private val mangaList: RecyclerView by bindView(R.id.mangaList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userId = arguments.getString(ARGUMENT_USER_ID)
        userName = arguments.getString(ARGUMENT_USER_NAME)
        animeAdapter = ToptenAdapter(savedInstanceState, ANIME)
        mangaAdapter = ToptenAdapter(savedInstanceState, MANGA)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_topten, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        animeList.isNestedScrollingEnabled = false
        animeList.layoutManager = GridLayoutManager(context, Utils.calculateSpanAmount(activity) + 1)
        animeList.adapter = animeAdapter
        mangaList.isNestedScrollingEnabled = false
        mangaList.layoutManager = GridLayoutManager(context, Utils.calculateSpanAmount(activity) + 1)
        mangaList.adapter = mangaAdapter

        show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        animeAdapter.saveInstanceState(outState)
        mangaAdapter.saveInstanceState(outState)
    }

    override fun constructLoadingRequest(): LoadingRequest<Array<Array<ToptenEntry>>> {
        return LoadingRequest(ToptenRequest(userId, userName, ANIME),
                ToptenRequest(userId, userName, MANGA), zipFunction = { partialResults ->

            @Suppress("UNCHECKED_CAST")
            RetainedLoadingFragment.LoadingResult(arrayOf(
                    partialResults[0] as Array<ToptenEntry>,
                    partialResults[1] as Array<ToptenEntry>
            ))
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