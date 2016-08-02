package com.proxerme.app.fragment

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import butterknife.bindView
import com.proxerme.app.R
import com.proxerme.app.adapter.ToptenAdapter
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.ProxerConnection
import com.proxerme.library.connection.user.request.ToptenRequest
import com.proxerme.library.info.ProxerTag
import com.proxerme.library.interfaces.ProxerErrorResult
import com.proxerme.library.interfaces.ProxerResult
import com.proxerme.library.parameters.CategoryParameter

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ToptenFragment : LoadingFragment() {

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

    override val parallelLoads = 2

    private val toptenContainer: ViewGroup by bindView(R.id.toptenContainer)
    private val animeContainer: ViewGroup by bindView(R.id.animeContainer)
    private val mangaContainer: ViewGroup by bindView(R.id.mangaContainer)
    private val animeList: RecyclerView by bindView(R.id.animeList)
    private val mangaList: RecyclerView by bindView(R.id.mangaList)
    override val errorContainer: ViewGroup by bindView(R.id.errorContainer)
    override val errorText: TextView by bindView(R.id.errorText)
    override val errorButton: Button by bindView(R.id.errorButton)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userId = arguments.getString(ARGUMENT_USER_ID)
        userName = arguments.getString(ARGUMENT_USER_NAME)
        animeAdapter = ToptenAdapter(savedInstanceState)
        mangaAdapter = ToptenAdapter(savedInstanceState)
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

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?,
                             savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_topten, container, false)
    }

    override fun cancel() {
        ProxerConnection.cancel(ProxerTag.TOPTEN)
    }

    override fun load(showProgress: Boolean) {
        super.load(showProgress)

        ToptenRequest(userId, userName, CategoryParameter.ANIME).execute({ result ->
            animeAdapter.setItems(result.item.asList())

            notifyLoadFinishedSuccessful(result)
        }, { result ->
            notifyLoadFinishedWithError(result)
        })

        ToptenRequest(userId, userName, CategoryParameter.MANGA).execute({ result ->
            mangaAdapter.setItems(result.item.asList())

            notifyLoadFinishedSuccessful(result)
        }, { result ->
            notifyLoadFinishedWithError(result)
        })
    }

    override fun showError(message: String, buttonMessage: String?,
                           onButtonClickListener: View.OnClickListener?) {
        super.showError(message, buttonMessage, onButtonClickListener)

        toptenContainer.visibility = View.INVISIBLE
    }

    override fun notifyLoadFinishedWithError(result: ProxerErrorResult) {
        super.notifyLoadFinishedWithError(result)

        show()
    }

    override fun notifyLoadFinishedSuccessful(result: ProxerResult<*>) {
        super.notifyLoadFinishedSuccessful(result)

        show()
    }

    private fun show() {
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