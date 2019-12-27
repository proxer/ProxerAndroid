package me.proxer.app.profile

import com.gojuno.koptional.None
import io.reactivex.Single
import io.reactivex.rxkotlin.plusAssign
import me.proxer.app.base.BaseViewModel
import me.proxer.app.profile.ProfileViewModel.UserInfoWrapper
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.entity.user.UserInfo

/**
 * @author Ruben Gees
 */
class ProfileViewModel(
    private val userId: String?,
    private val username: String?
) : BaseViewModel<UserInfoWrapper>() {

    override val dataSingle: Single<UserInfoWrapper>
        get() = Single.fromCallable { validate() }
            .flatMap { api.user.info(userId, username).buildSingle() }
            .flatMap { userInfo ->
                val maybeUcpSingle = when (storageHelper.user?.matches(userId, username) == true) {
                    true -> api.ucp.watchedEpisodes().buildOptionalSingle()
                    false -> Single.just(None)
                }

                maybeUcpSingle.map { watchedEpisodes -> UserInfoWrapper(userInfo, watchedEpisodes.toNullable()) }
            }

    init {
        disposables += storageHelper.isLoggedInObservable.subscribe { reload() }
    }

    data class UserInfoWrapper(val info: UserInfo, val watchedEpisodes: Int?)
}
