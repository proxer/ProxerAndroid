package com.proxerme.app.manager

import com.proxerme.app.application.MainApplication
import com.proxerme.app.event.LoginFailedEvent
import com.proxerme.app.event.LogoutFailedEvent
import com.proxerme.app.helper.StorageHelper
import com.proxerme.library.connection.ProxerCall
import com.proxerme.library.connection.ProxerException
import com.proxerme.library.connection.user.entitiy.User
import com.proxerme.library.connection.user.request.LoginRequest
import com.proxerme.library.connection.user.request.LogoutRequest
import com.proxerme.library.connection.user.request.UserInfoRequest
import org.greenrobot.eventbus.EventBus
import org.joda.time.DateTime
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.properties.Delegates

/**
 * A singleton for managing the user and it's login state.

 * @author Ruben Gees
 */
object UserManager {

    private const val RELOGIN_THRESHOLD = 45

    var user: User? = StorageHelper.user
        private set

    var loginState: LoginState by Delegates.observable(LoginState.LOGGED_OUT, { property, old,
                                                                                new ->
        if (new != old) {
            EventBus.getDefault().post(new)
        }
    })
        private set

    var ongoingState: OngoingState by Delegates.observable(OngoingState.NONE, { property, old,
                                                                                new ->
        if (new != old) {
            EventBus.getDefault().post(new)
        }
    })
        private set

    enum class LoginState {
        LOGGED_IN, LOGGED_OUT
    }

    enum class OngoingState {
        NONE, LOGGING_IN, LOGGING_OUT
    }

    enum class SaveOption {
        SAVE, DONT_SAVE, SAME_AS_IS
    }

    private val requests: MutableList<ProxerCall> = ArrayList()
    private val lock = ReentrantLock()

    fun login(user: User, shouldSave: SaveOption) {
        lock.lock()

        if (ongoingState != OngoingState.LOGGING_IN) {
            cancelAndClearRequests()

            ongoingState = OngoingState.LOGGING_IN

            requests.add(MainApplication.proxerConnection.execute(
                    LoginRequest(user.username, user.password),
                    { result ->
                        lock.lock()

                        cancelAndClearRequests()
                        doLogin(result, shouldSave)

                        lock.unlock()
                    },
                    { result ->
                        lock.lock()

                        if (result.errorCode == ProxerException.PROXER &&
                                result.proxerErrorCode == ProxerException.LOGIN_ALREADY_LOGGED_IN) {
                            requests.add(MainApplication.proxerConnection.execute(
                                    UserInfoRequest(null, user.username),
                                    { userInfoResult ->
                                        lock.lock()

                                        val newUser = User(user.username, user.password,
                                                userInfoResult.id, userInfoResult.imageId)

                                        cancelAndClearRequests()
                                        doLogin(newUser, shouldSave)

                                        lock.unlock()
                                    },
                                    { userInfoResult ->
                                        lock.lock()

                                        cancelAndClearRequests()
                                        doLogout()

                                        EventBus.getDefault().post(LoginFailedEvent(userInfoResult))

                                        lock.unlock()
                                    }))
                        } else {
                            cancelAndClearRequests()
                            doLogout()

                            EventBus.getDefault().post(LoginFailedEvent(result))
                        }

                        lock.unlock()
                    }))
        }

        lock.unlock()
    }

    fun reLogin() {
        lock.lock()

        if (user != null && ongoingState != OngoingState.LOGGING_IN) {
            val lastLoginTime = StorageHelper.lastLoginTime

            cancelAndClearRequests()

            if (DateTime(lastLoginTime).isBefore(DateTime().minusMinutes(RELOGIN_THRESHOLD))) {
                ongoingState = OngoingState.LOGGING_IN

                user?.apply {
                    MainApplication.proxerConnection.execute(
                            LoginRequest(this.username, this.password),
                            { result ->
                                lock.lock()

                                cancelAndClearRequests()
                                doLogin(result, SaveOption.SAME_AS_IS)

                                lock.unlock()
                            },
                            { result ->
                                lock.lock()

                                cancelAndClearRequests()
                                if (result.errorCode == ProxerException.PROXER &&
                                        result.proxerErrorCode ==
                                                ProxerException.LOGIN_ALREADY_LOGGED_IN) {
                                    doLogin(this, SaveOption.SAME_AS_IS)
                                } else {
                                    doLogout()

                                    EventBus.getDefault().post(LoginFailedEvent(result))
                                }

                                lock.unlock()
                            })
                }
            } else {
                ongoingState = OngoingState.NONE
                loginState = LoginState.LOGGED_IN
            }
        }

        lock.unlock()
    }

    @Throws(ProxerException::class)
    @Synchronized
    fun reLoginSync() {
        lock.lock()

        if (user == null || ongoingState == OngoingState.LOGGING_OUT) {
            throw ProxerException(ProxerException.CANCELLED)
        }

        val lastLogin = StorageHelper.lastLoginTime

        cancelAndClearRequests()

        if (DateTime(lastLogin).isBefore(DateTime().minusMinutes(RELOGIN_THRESHOLD))) {
            ongoingState = OngoingState.LOGGING_IN

            user?.apply {
                try {
                    lock.unlock()

                    val result = MainApplication.proxerConnection.executeSynchronized(LoginRequest(
                            this.username, this.password))

                    lock.lock()

                    doLogin(result, SaveOption.SAME_AS_IS)
                } catch (exception: ProxerException) {
                    lock.lock()

                    if (exception.errorCode == ProxerException.PROXER) {
                        if (exception.proxerErrorCode == ProxerException.LOGIN_ALREADY_LOGGED_IN) {
                            doLogin(this, SaveOption.SAME_AS_IS)
                        } else {
                            doLogout()

                            EventBus.getDefault().post(LoginFailedEvent(exception))
                        }
                    } else {
                        doLogout()

                        EventBus.getDefault().post(LoginFailedEvent(exception))
                    }
                }
            }
        } else {
            ongoingState = OngoingState.NONE
            loginState = LoginState.LOGGED_IN
        }

        lock.unlock()
    }

    fun logout() {
        lock.lock()

        if (ongoingState != OngoingState.LOGGING_OUT) {
            ongoingState = OngoingState.LOGGING_OUT

            cancelAndClearRequests()

            requests.add(MainApplication.proxerConnection.execute(LogoutRequest(),
                    { result ->
                        lock.lock()

                        cancelAndClearRequests()
                        removeUser()
                        doLogout()

                        lock.unlock()
                    },
                    { result ->
                        lock.lock()

                        ongoingState = OngoingState.NONE

                        EventBus.getDefault().post(LogoutFailedEvent(result))

                        lock.unlock()
                    }))
        }

        lock.unlock()
    }

    fun cancel() {
        lock.lock()

        cancelAndClearRequests()
        ongoingState = OngoingState.NONE

        lock.unlock()
    }

    fun notifyLoggedOut() {
        lock.lock()

        cancelAndClearRequests()
        doLogout()

        lock.unlock()
    }

    private fun cancelAndClearRequests() {
        requests.forEach(ProxerCall::cancel)

        requests.clear()
    }

    private fun doLogin(user: User, shouldSave: SaveOption) {
        changeUser(user, shouldSave)

        loginState = LoginState.LOGGED_IN
        ongoingState = OngoingState.NONE
        StorageHelper.lastLoginTime = System.currentTimeMillis()
    }

    private fun doLogout() {
        loginState = LoginState.LOGGED_OUT
        ongoingState = OngoingState.NONE
        StorageHelper.lastLoginTime = -1L
    }

    private fun removeUser() {
        this.user = null

        StorageHelper.user = null
    }

    private fun changeUser(user: User, shouldSave: SaveOption) {
        this.user = user

        if (shouldSave == SaveOption.SAVE) {
            StorageHelper.user = user
        } else if (shouldSave == SaveOption.DONT_SAVE) {
            StorageHelper.user = null
        }
    }
}
