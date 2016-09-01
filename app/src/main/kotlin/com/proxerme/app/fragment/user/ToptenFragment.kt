package com.proxerme.app.fragment.user

import android.view.View
import butterknife.bindView
import com.proxerme.library.connection.user.entitiy.ToptenEntry
import com.proxerme.library.parameters.CategoryParameter.ANIME
import com.proxerme.library.parameters.CategoryParameter.MANGA

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ToptenFragment : com.proxerme.app.fragment.framework.EasyLoadingFragment<Array<Array<com.proxerme.library.connection.user.entitiy.ToptenEntry>>>() {

    companion object {
        private const val ARGUMENT_USER_ID = "user_id"
        private const val ARGUMENT_USER_NAME = "user_name"

        fun newInstance(userId: String? = null, userName: String? = null): com.proxerme.app.fragment.user.ToptenFragment {
            if (userId.isNullOrBlank() && userName.isNullOrBlank()) {
                throw IllegalArgumentException("You must provide at least one of the arguments")
            }

            return com.proxerme.app.fragment.user.ToptenFragment().apply {
                this.arguments = android.os.Bundle().apply {
                    this.putString(com.proxerme.app.fragment.user.ToptenFragment.Companion.ARGUMENT_USER_ID, userId)
                    this.putString(com.proxerme.app.fragment.user.ToptenFragment.Companion.ARGUMENT_USER_NAME, userName)
                }
            }
        }
    }

    override val section = com.proxerme.app.manager.SectionManager.Section.TOPTEN

    private var userId: String? = null
    private var userName: String? = null

    private lateinit var animeAdapter: com.proxerme.app.adapter.ToptenAdapter
    private lateinit var mangaAdapter: com.proxerme.app.adapter.ToptenAdapter

    private val animeContainer: android.view.ViewGroup by bindView(com.proxerme.app.R.id.animeContainer)
    private val mangaContainer: android.view.ViewGroup by bindView(com.proxerme.app.R.id.mangaContainer)
    private val animeList: android.support.v7.widget.RecyclerView by bindView(com.proxerme.app.R.id.animeList)
    private val mangaList: android.support.v7.widget.RecyclerView by bindView(com.proxerme.app.R.id.mangaList)

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        userId = arguments.getString(com.proxerme.app.fragment.user.ToptenFragment.Companion.ARGUMENT_USER_ID)
        userName = arguments.getString(com.proxerme.app.fragment.user.ToptenFragment.Companion.ARGUMENT_USER_NAME)
        animeAdapter = com.proxerme.app.adapter.ToptenAdapter(savedInstanceState, ANIME)
        mangaAdapter = com.proxerme.app.adapter.ToptenAdapter(savedInstanceState, MANGA)
    }

    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?,
                              savedInstanceState: android.os.Bundle?): android.view.View {
        return inflater.inflate(com.proxerme.app.R.layout.fragment_topten, container, false)
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        animeList.isNestedScrollingEnabled = false
        animeList.layoutManager = android.support.v7.widget.GridLayoutManager(context, com.proxerme.app.util.Utils.calculateSpanAmount(activity) + 1)
        animeList.adapter = animeAdapter
        mangaList.isNestedScrollingEnabled = false
        mangaList.layoutManager = android.support.v7.widget.GridLayoutManager(context, com.proxerme.app.util.Utils.calculateSpanAmount(activity) + 1)
        mangaList.adapter = mangaAdapter

        show()
    }

    override fun onSaveInstanceState(outState: android.os.Bundle) {
        super.onSaveInstanceState(outState)

        animeAdapter.saveInstanceState(outState)
        mangaAdapter.saveInstanceState(outState)
    }

    override fun constructLoadingRequest(): LoadingRequest<Array<Array<com.proxerme.library.connection.user.entitiy.ToptenEntry>>> {
        return LoadingRequest(com.proxerme.library.connection.user.request.ToptenRequest(userId, userName, ANIME),
                com.proxerme.library.connection.user.request.ToptenRequest(userId, userName, MANGA), zipFunction = { partialResults ->

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