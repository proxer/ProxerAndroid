package com.proxerme.app.manager

import com.proxerme.app.application.MainApplication
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
import kotlin.properties.Delegates

/**
 * A singleton for managing the user and it's login state.

 * @author Ruben Gees
 */
object UserManager {

    private const val RELOGIN_THRESHOLD = 45

    var user: User? = StorageHelper.user
        private set

    var loginState: LoginState by Delegates.observable(LoginState.LOGGED_OUT, { property, old, new ->
        if (new != old) {
            EventBus.getDefault().post(new)
        }
    })
        private set

    var ongoingState: OngoingState by Delegates.observable(OngoingState.NONE, { property, old, new ->
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

    val requests: MutableList<ProxerCall> = Collections.synchronizedList(ArrayList<ProxerCall>())

    fun login(user: User, shouldSave: SaveOption, callback: ((User) -> Unit)? = null,
              errorCallback: ((ProxerException) -> Unit)? = null) {
        if (ongoingState != OngoingState.LOGGING_IN) {
            ongoingState = OngoingState.LOGGING_IN

            cancel()

            requests.add(MainApplication.proxerConnection.execute(
                    LoginRequest(user.username, user.password),
                    { result ->
                        requests.clear()

                        doLogin(result, shouldSave)
                        callback?.invoke(result)
                    },
                    { result ->
                        if (result.errorCode == ProxerException.PROXER &&
                                result.proxerErrorCode == ProxerException.LOGIN_ALREADY_LOGGED_IN) {
                            requests.add(MainApplication.proxerConnection.execute(
                                    UserInfoRequest(null, user.username),
                                    { userInfoResult ->
                                        requests.clear()

                                        val newUser = User(user.username, user.password,
                                                userInfoResult.id, userInfoResult.imageId)

                                        doLogin(newUser, shouldSave)
                                        callback?.invoke(newUser)
                                    },
                                    { userInfoResult ->
                                        requests.clear()
                                        doLogout()

                                        errorCallback?.invoke(userInfoResult)
                                    }))
                        } else {
                            requests.clear()
                            doLogout()

                            errorCallback?.invoke(result)
                        }
                    }))
        }
    }

    fun reLogin() {
        if (user != null && ongoingState != OngoingState.LOGGING_IN) {
            val lastLoginTime = StorageHelper.lastLoginTime

            cancel()

            if (DateTime(lastLoginTime).isBefore(DateTime().minusMinutes(RELOGIN_THRESHOLD))) {
                ongoingState = OngoingState.LOGGING_IN

                user?.apply {
                    MainApplication.proxerConnection.execute(
                            LoginRequest(this.username, this.password),
                            { result ->
                                requests.clear()

                                doLogin(result, SaveOption.SAME_AS_IS)
                            },
                            { result ->
                                requests.clear()

                                if (result.errorCode == ProxerException.PROXER &&
                                        result.proxerErrorCode ==
                                                ProxerException.LOGIN_ALREADY_LOGGED_IN) {
                                    doLogin(this, SaveOption.SAME_AS_IS)
                                } else {
                                    doLogout()
                                }
                            })
                }
            } else {
                ongoingState = OngoingState.NONE
                loginState = LoginState.LOGGED_IN
            }
        }
    }

    @Throws(ProxerException::class)
    @Synchronized
    fun reLoginSync() {
        if (user == null) {
            throw RuntimeException("No user available")
        }

        while (ongoingState != OngoingState.NONE) {
            Thread.sleep(5)
        }

        ongoingState = OngoingState.LOGGING_IN
        val lastLogin = StorageHelper.lastLoginTime

        if (DateTime(lastLogin).isBefore(DateTime().minusMinutes(RELOGIN_THRESHOLD))) {
            ongoingState = OngoingState.LOGGING_IN

            user?.apply {
                try {
                    doLogin(MainApplication.proxerConnection.executeSynchronized(
                            LoginRequest(this.username, this.password)), SaveOption.SAME_AS_IS)
                } catch (exception: ProxerException) {
                    if (exception.errorCode == ProxerException.PROXER) {
                        if (exception.proxerErrorCode == ProxerException.LOGIN_ALREADY_LOGGED_IN) {
                            doLogin(this, SaveOption.SAME_AS_IS)
                        } else {
                            doLogout()
                        }
                    } else {
                        doLogout()
                    }
                }
            }
        } else {
            ongoingState = OngoingState.NONE
            loginState = LoginState.LOGGED_IN
        }
    }

    fun logout(callback: (() -> Unit)? = null,
               errorCallback: ((ProxerException) -> Unit)?) {
        if (ongoingState != OngoingState.LOGGING_OUT) {
            ongoingState = OngoingState.LOGGING_OUT

            cancel()

            requests.add(MainApplication.proxerConnection.execute(LogoutRequest(),
                    { result ->
                        requests.clear()

                        removeUser()
                        doLogout()
                        callback?.invoke()
                    },
                    { result ->
                        ongoingState = OngoingState.NONE
                        errorCallback?.invoke(result)
                    }))
        }
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

    fun cancel() {
        requests.forEach {
            it.cancel()
        }

        requests.clear()
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
