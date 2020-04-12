package me.proxer.app.chat.prv.message

import com.gojuno.koptional.Optional
import io.reactivex.Single
import me.proxer.app.chat.ReportViewModel
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.safeInject
import me.proxer.library.ProxerApi
import org.koin.core.KoinComponent

class MessengerReportViewModel : ReportViewModel(), KoinComponent {

    private val api by safeInject<ProxerApi>()

    override fun reportSingle(id: String, message: String): Single<Optional<Unit>> {
        return api.messenger.report(id, message).buildOptionalSingle()
    }
}
