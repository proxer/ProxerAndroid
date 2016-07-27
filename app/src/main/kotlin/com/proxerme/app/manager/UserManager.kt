package com.proxerme.app.manager

import com.proxerme.app.helper.StorageHelper
import com.proxerme.library.connection.ProxerConnection
import com.proxerme.library.connection.ProxerException
import com.proxerme.library.connection.user.entitiy.User
import com.proxerme.library.connection.user.request.LoginRequest
import com.proxerme.library.connection.user.request.LogoutRequest
import com.proxerme.library.connection.user.request.UserInfoRequest
import com.proxerme.library.connection.user.result.LoginResult
import com.proxerme.library.connection.user.result.LogoutResult
import com.proxerme.library.info.ProxerTag
import com.proxerme.library.interfaces.ProxerErrorResult
import org.greenrobot.eventbus.EventBus
import org.joda.time.DateTime
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
        EventBus.getDefault().post(new)
    })
        private set

    var ongoingState: OngoingState by Delegates.observable(OngoingState.NONE, { property, old, new ->
        EventBus.getDefault().post(new)
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

    fun login(user: User, shouldSave: SaveOption, callback: ((LoginResult) -> Unit)? = null,
              errorCallback: ((ProxerErrorResult) -> Unit)? = null) {
        if (ongoingState != OngoingState.LOGGING_IN) {
            ongoingState = OngoingState.LOGGING_IN

            cancel()

            LoginRequest(user).execute({ result ->
                doLogin(result.item, shouldSave)
                callback?.invoke(result)
            }, { result ->
                if (result.item.errorCode == ProxerException.PROXER &&
                        result.item.proxerErrorCode ==
                                ProxerException.LOGIN_ALREADY_LOGGED_IN) {
                    UserInfoRequest(null, user.username).execute({ userInfoResult ->
                        val newUser = User(user.username, user.password, userInfoResult.item.id,
                                userInfoResult.item.imageId)

                        doLogin(newUser, shouldSave)
                        callback?.invoke(LoginResult(newUser))
                    }, { userInfoResult ->
                        errorCallback?.invoke(userInfoResult)
                    })
                } else {
                    doLogout()
                    errorCallback?.invoke(result)
                }
            })
        }
    }

    fun reLogin() {
        if (user != null && ongoingState != OngoingState.LOGGING_IN) {
            val lastLoginTime = StorageHelper.lastLoginTime

            cancel()

            if (DateTime(lastLoginTime).isBefore(DateTime().minusMinutes(RELOGIN_THRESHOLD))) {
                ongoingState = OngoingState.LOGGING_IN

                user?.run {
                    LoginRequest(this).execute({ result ->
                        doLogin(result.item, SaveOption.SAME_AS_IS)
                    }, { result ->
                        if (result.item.errorCode == ProxerException.PROXER &&
                                result.item.proxerErrorCode ==
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

            user?.run {
                try {
                    doLogin(LoginRequest(this).executeSynchronized().item,
                            SaveOption.SAME_AS_IS)
                } catch (exception: ProxerException) {
                    if (exception.errorCode == ProxerException.PROXER &&
                            exception.proxerErrorCode ==
                                    ProxerException.LOGIN_ALREADY_LOGGED_IN) {
                        doLogin(this, SaveOption.SAME_AS_IS)
                    } else {
                        doLogout()

                        throw exception
                    }
                }
            }
        } else {
            ongoingState = OngoingState.NONE
            loginState = LoginState.LOGGED_IN
        }
    }

    fun logout(callback: ((LogoutResult) -> Unit)? = null,
               errorCallback: ((ProxerErrorResult) -> Unit)?) {
        if (ongoingState != OngoingState.LOGGING_OUT) {
            ongoingState = OngoingState.LOGGING_OUT

            cancel()

            LogoutRequest().execute({ result ->
                doLogout()
                callback?.invoke(result)
            }, { result ->
                removeUser()
                ongoingState = OngoingState.NONE
                errorCallback?.invoke(result)
            })
        }
    }

    private fun doLogin(user: User, shouldSave: SaveOption) {
        changeUser(user, shouldSave)

        loginState = LoginState.LOGGED_IN
        ongoingState = OngoingState.NONE
        StorageHelper.lastLoginTime = System.currentTimeMillis()
    }

    private fun doLogout() {
        removeUser()

        loginState = LoginState.LOGGED_OUT
        ongoingState = OngoingState.NONE
        StorageHelper.lastLoginTime = -1L
    }

    fun cancel() {
        ProxerConnection.cancel(ProxerTag.LOGOUT)
        ProxerConnection.cancel(ProxerTag.LOGIN)
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
