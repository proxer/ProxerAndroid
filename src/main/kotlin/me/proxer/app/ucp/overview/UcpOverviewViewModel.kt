package me.proxer.app.ucp.overview

import com.gojuno.koptional.Optional
import io.reactivex.Single
import me.proxer.app.base.BaseViewModel
import me.proxer.app.util.extension.buildOptionalSingle

/**
 * @author Ruben Gees
 */
class UcpOverviewViewModel : BaseViewModel<Optional<Int>>() {

    override val isLoginRequired = true

    override val dataSingle: Single<Optional<Int>>
        get() = Single.fromCallable { validate() }
            .flatMap { api.ucp.watchedEpisodes().buildOptionalSingle() }
}
