package me.proxer.app.chat.prv.message

import io.reactivex.Single
import me.proxer.app.chat.ReportViewModel
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.chat.prv.sync.MessengerDatabase
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.safeInject
import me.proxer.library.ProxerApi

class MessengerReportViewModel : ReportViewModel() {

    private val api by safeInject<ProxerApi>()
    private val messengerDao by safeInject<MessengerDao>()
    private val messengerDatabase by safeInject<MessengerDatabase>()

    override fun reportSingle(id: String, message: String): Single<Unit> {
        return api.messenger.report(id, message).buildSingle()
            .map {
                messengerDatabase.runInTransaction {
                    val conference = messengerDao.getConference(id.toLong())

                    if (!conference.isGroup) {
                        messengerDao.deleteMessagesByConferenceId(id)
                        messengerDao.deleteConferenceById(id)
                    }
                }
            }
    }
}
