package me.proxer.app.profile.history

import io.reactivex.Single
import io.reactivex.rxkotlin.plusAssign
import me.proxer.app.base.PagedViewModel
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toLocalEntry
import me.proxer.app.util.extension.toLocalEntryUcp

/**
 * @author Ruben Gees
 */
class HistoryViewModel(
    private val userId: String?,
    private val username: String?
) : PagedViewModel<LocalUserHistoryEntry>() {

    override val itemsOnPage = 50

    override val dataSingle: Single<List<LocalUserHistoryEntry>>
        get() = Single.fromCallable { validate() }
            .flatMap {
                when (storageHelper.user?.matches(userId, username) == true) {
                    true ->
                        api.ucp.history()
                            .page(page)
                            .limit(itemsOnPage)
                            .buildSingle()
                            .map { entries -> entries.map { it.toLocalEntryUcp() } }
                    false ->
                        api.user.history(userId, username)
                            .includeHentai(preferenceHelper.isAgeRestrictedMediaAllowed && storageHelper.isLoggedIn)
                            .page(page)
                            .limit(itemsOnPage)
                            .buildSingle()
                            .map { entries -> entries.map { it.toLocalEntry() } }
                }
            }

    init {
        disposables += storageHelper.isLoggedInObservable.subscribe { reload() }
    }
}
