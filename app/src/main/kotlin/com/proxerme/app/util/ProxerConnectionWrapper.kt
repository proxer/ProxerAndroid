package com.proxerme.app.util

import com.proxerme.app.BuildConfig
import com.proxerme.app.entitiy.LocalUser
import com.proxerme.app.event.LoginEvent
import com.proxerme.app.event.LogoutEvent
import com.proxerme.app.helper.StorageHelper
import com.proxerme.library.connection.ProxerCall
import com.proxerme.library.connection.ProxerConnection
import com.proxerme.library.connection.ProxerException
import com.proxerme.library.connection.ProxerRequest
import com.proxerme.library.connection.user.request.LoginRequest
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import org.greenrobot.eventbus.EventBus

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
object ProxerConnectionWrapper {

    private const val USER_AGENT = "ProxerAndroid/${BuildConfig.VERSION_NAME}"

    val httpClient: OkHttpClient
        get() = proxerConnection.httpClient

    val moshi: Moshi
        get() = proxerConnection.moshi

    private val proxerConnection = ProxerConnection.Builder(BuildConfig.PROXER_API_KEY)
            .withCustomUserAgent(USER_AGENT)
            .build()

    fun <T> exec(request: ProxerRequest<T>, callback: (T) -> Unit,
                 errorCallback: (ProxerException) -> Unit): ProxerTokenCall<T> {
        return ProxerTokenCall(request, callback, errorCallback)
    }

    @Throws(ProxerException::class)
    fun <T> execSync(request: ProxerRequest<T>): T {
        try {
            return proxerConnection.executeSynchronized(request, StorageHelper.user?.loginToken)
        } catch (exception: ProxerException) {
            when (exception.proxerErrorCode) {
                in ErrorUtils.NOT_LOGGED_IN_ERRORS -> {
                    val user = StorageHelper.user

                    if (user?.password != null) {
                        val result = try {
                            proxerConnection.executeSynchronized(LoginRequest(user.username,
                                    user.password))
                        } catch (exception: ProxerException) {
                            when (exception.proxerErrorCode) {
                                ProxerException.LOGIN_INVALID_CREDENTIALS,
                                ProxerException.LOGIN_MISSING_CREDENTIALS -> {
                                    StorageHelper.user = null

                                    EventBus.getDefault().post(LogoutEvent())
                                }
                            }

                            throw exception
                        }

                        StorageHelper.user = LocalUser(user.username, user.password,
                                result.id, result.imageId, result.loginToken)

                        EventBus.getDefault().post(LoginEvent())

                        return proxerConnection.executeSynchronized(request, result.loginToken)
                    } else {
                        StorageHelper.user = null

                        EventBus.getDefault().post(LogoutEvent())

                        throw exception
                    }
                }
                else -> throw exception
            }
        }
    }

    class ProxerTokenCall<T>(request: ProxerRequest<T>, callback: (T) -> Unit,
                             errorCallback: (ProxerException) -> Unit) {

        private val calls = ArrayList<ProxerCall>(3)

        @Volatile
        private var cancelled = false

        init {
            calls += proxerConnection.execute(request, StorageHelper.user?.loginToken, callback, {
                when (it.proxerErrorCode) {
                    in ErrorUtils.NOT_LOGGED_IN_ERRORS -> {
                        if (!cancelled) {
                            val user = StorageHelper.user

                            if (user?.password != null) {
                                calls += proxerConnection.execute(LoginRequest(user.username,
                                        user.password), {
                                    if (!cancelled) {
                                        StorageHelper.user = LocalUser(user.username, user.password,
                                                it.id, it.imageId, it.loginToken)

                                        EventBus.getDefault().post(LoginEvent())

                                        calls += proxerConnection.execute(request, it.loginToken,
                                                callback, errorCallback)
                                    }
                                }, {
                                    if (!cancelled) {
                                        when (it.proxerErrorCode) {
                                            ProxerException.LOGIN_INVALID_CREDENTIALS,
                                            ProxerException.LOGIN_MISSING_CREDENTIALS -> {
                                                StorageHelper.user = null

                                                EventBus.getDefault().post(LogoutEvent())
                                            }
                                        }

                                        errorCallback.invoke(it)
                                    }
                                })
                            } else {
                                StorageHelper.user = null

                                EventBus.getDefault().post(LogoutEvent())

                                errorCallback.invoke(it)
                            }
                        }
                    }
                    else -> {
                        if (!cancelled) {
                            errorCallback.invoke(it)
                        }
                    }
                }
            })
        }

        fun cancel() {
            cancelled = true

            calls.forEach { it.cancel() }
            calls.clear()
        }
    }
}
