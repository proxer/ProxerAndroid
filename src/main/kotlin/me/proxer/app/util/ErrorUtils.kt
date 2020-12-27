package me.proxer.app.util

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.View
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.Loader
import me.proxer.app.R
import me.proxer.app.auth.LoginDialog
import me.proxer.app.base.BaseActivity
import me.proxer.app.comment.CommentInvalidProgressException
import me.proxer.app.comment.CommentTooLongException
import me.proxer.app.exception.AgeConfirmationRequiredException
import me.proxer.app.exception.ChatException
import me.proxer.app.exception.NotConnectedException
import me.proxer.app.exception.NotLoggedInException
import me.proxer.app.exception.PartialException
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.manga.MangaLinkException
import me.proxer.app.manga.MangaNotAvailableException
import me.proxer.app.settings.AgeConfirmationDialog
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction.AGE_CONFIRMATION
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction.CAPTCHA
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction.LOGIN
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction.NETWORK_SETTINGS
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction.OPEN_LINK
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_DEFAULT
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.safeInject
import me.proxer.library.ProxerException
import me.proxer.library.ProxerException.ErrorType.CANCELLED
import me.proxer.library.ProxerException.ErrorType.IO
import me.proxer.library.ProxerException.ErrorType.PARSING
import me.proxer.library.ProxerException.ErrorType.SERVER
import me.proxer.library.ProxerException.ErrorType.TIMEOUT
import me.proxer.library.ProxerException.ErrorType.UNKNOWN
import me.proxer.library.ProxerException.ServerErrorType.ANIME_INVALID_EPISODE
import me.proxer.library.ProxerException.ServerErrorType.ANIME_INVALID_STREAM
import me.proxer.library.ProxerException.ServerErrorType.ANIME_LOGIN_REQUIRED
import me.proxer.library.ProxerException.ServerErrorType.API_MAINTENANCE
import me.proxer.library.ProxerException.ServerErrorType.API_REMOVED
import me.proxer.library.ProxerException.ServerErrorType.APPS_INVALID_ID
import me.proxer.library.ProxerException.ServerErrorType.AUTH_CODE_ALREADY_EXISTS
import me.proxer.library.ProxerException.ServerErrorType.AUTH_CODE_DOES_NOT_EXIST
import me.proxer.library.ProxerException.ServerErrorType.AUTH_CODE_DUPLICATE
import me.proxer.library.ProxerException.ServerErrorType.AUTH_CODE_INVALID_NAME
import me.proxer.library.ProxerException.ServerErrorType.AUTH_CODE_PENDING
import me.proxer.library.ProxerException.ServerErrorType.AUTH_CODE_REJECTED
import me.proxer.library.ProxerException.ServerErrorType.AUTH_INVALID_USER
import me.proxer.library.ProxerException.ServerErrorType.CHAT_INVALID_INPUT
import me.proxer.library.ProxerException.ServerErrorType.CHAT_INVALID_MESSAGE
import me.proxer.library.ProxerException.ServerErrorType.CHAT_INVALID_PERMISSIONS
import me.proxer.library.ProxerException.ServerErrorType.CHAT_INVALID_ROOM
import me.proxer.library.ProxerException.ServerErrorType.CHAT_INVALID_THANK_YOU
import me.proxer.library.ProxerException.ServerErrorType.CHAT_LOGIN_REQUIRED
import me.proxer.library.ProxerException.ServerErrorType.CHAT_NO_PERMISSIONS
import me.proxer.library.ProxerException.ServerErrorType.CHAT_SEVEN_DAY_PROTECTION
import me.proxer.library.ProxerException.ServerErrorType.CHAT_USER_ON_BLACKLIST
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_ALREADY_EXISTS
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_INSUFFICIENT_PERMISSIONS
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_INVALID_COMMENT
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_INVALID_CONTENT
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_INVALID_ENTRY_ID
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_INVALID_EPISODE
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_INVALID_ID
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_INVALID_RATING
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_INVALID_STATUS
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_LOGIN_REQUIRED
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_NOT_ACTIVE_YET
import me.proxer.library.ProxerException.ServerErrorType.COMMENT_SAVE_ERROR
import me.proxer.library.ProxerException.ServerErrorType.ERRORLOG_INVALID_INPUT
import me.proxer.library.ProxerException.ServerErrorType.FORUM_INVALID_ID
import me.proxer.library.ProxerException.ServerErrorType.FORUM_INVALID_PERMISSIONS
import me.proxer.library.ProxerException.ServerErrorType.FUNCTION_BLOCKED
import me.proxer.library.ProxerException.ServerErrorType.INFO_DELETE_COMMENT_INVALID_INPUT
import me.proxer.library.ProxerException.ServerErrorType.INFO_ENTRY_ALREADY_IN_LIST
import me.proxer.library.ProxerException.ServerErrorType.INFO_EXCEEDED_MAXIMUM_ENTRIES
import me.proxer.library.ProxerException.ServerErrorType.INFO_INVALID_ID
import me.proxer.library.ProxerException.ServerErrorType.INFO_INVALID_TYPE
import me.proxer.library.ProxerException.ServerErrorType.INFO_LOGIN_REQUIRED
import me.proxer.library.ProxerException.ServerErrorType.INSUFFICIENT_PERMISSIONS
import me.proxer.library.ProxerException.ServerErrorType.INTERNAL
import me.proxer.library.ProxerException.ServerErrorType.INVALID_API_CLASS
import me.proxer.library.ProxerException.ServerErrorType.INVALID_API_FUNCTION
import me.proxer.library.ProxerException.ServerErrorType.INVALID_TOKEN
import me.proxer.library.ProxerException.ServerErrorType.IP_AUTHENTICATION_REQUIRED
import me.proxer.library.ProxerException.ServerErrorType.IP_BLOCKED
import me.proxer.library.ProxerException.ServerErrorType.LIST_INVALID_CATEGORY
import me.proxer.library.ProxerException.ServerErrorType.LIST_INVALID_ID
import me.proxer.library.ProxerException.ServerErrorType.LIST_INVALID_LANGUAGE
import me.proxer.library.ProxerException.ServerErrorType.LIST_INVALID_MEDIUM
import me.proxer.library.ProxerException.ServerErrorType.LIST_INVALID_SUBJECT
import me.proxer.library.ProxerException.ServerErrorType.LIST_INVALID_TYPE
import me.proxer.library.ProxerException.ServerErrorType.LIST_NO_ENTRIES_LEFT
import me.proxer.library.ProxerException.ServerErrorType.LIST_TOP_ACCESS_RESET
import me.proxer.library.ProxerException.ServerErrorType.LOGIN_ALREADY_LOGGED_IN
import me.proxer.library.ProxerException.ServerErrorType.LOGIN_DIFFERENT_USER_ALREADY_LOGGED_IN
import me.proxer.library.ProxerException.ServerErrorType.LOGIN_INVALID_CREDENTIALS
import me.proxer.library.ProxerException.ServerErrorType.LOGIN_MISSING_CREDENTIALS
import me.proxer.library.ProxerException.ServerErrorType.MANGA_INVALID_CHAPTER
import me.proxer.library.ProxerException.ServerErrorType.MEDIA_INVALID_ENTRY
import me.proxer.library.ProxerException.ServerErrorType.MEDIA_INVALID_STYLE
import me.proxer.library.ProxerException.ServerErrorType.MEDIA_REMOVED_DUE_TO_COPYRIGHT
import me.proxer.library.ProxerException.ServerErrorType.MESSAGES_EXCEEDED_MAXIMUM_USERS
import me.proxer.library.ProxerException.ServerErrorType.MESSAGES_INVALID_CONFERENCE
import me.proxer.library.ProxerException.ServerErrorType.MESSAGES_INVALID_MESSAGE
import me.proxer.library.ProxerException.ServerErrorType.MESSAGES_INVALID_REPORT_INPUT
import me.proxer.library.ProxerException.ServerErrorType.MESSAGES_INVALID_TOPIC
import me.proxer.library.ProxerException.ServerErrorType.MESSAGES_INVALID_USER
import me.proxer.library.ProxerException.ServerErrorType.MESSAGES_LOGIN_REQUIRED
import me.proxer.library.ProxerException.ServerErrorType.MESSAGES_MISSING_USER
import me.proxer.library.ProxerException.ServerErrorType.NEWS
import me.proxer.library.ProxerException.ServerErrorType.NOTIFICATIONS_LOGIN_REQUIRED
import me.proxer.library.ProxerException.ServerErrorType.RATE_LIMIT
import me.proxer.library.ProxerException.ServerErrorType.SERVER_MAINTENANCE
import me.proxer.library.ProxerException.ServerErrorType.UCP_INVALID_CATEGORY
import me.proxer.library.ProxerException.ServerErrorType.UCP_INVALID_EPISODE
import me.proxer.library.ProxerException.ServerErrorType.UCP_INVALID_ID
import me.proxer.library.ProxerException.ServerErrorType.UCP_INVALID_SETTINGS
import me.proxer.library.ProxerException.ServerErrorType.UCP_LOGIN_REQUIRED
import me.proxer.library.ProxerException.ServerErrorType.UNKNOWN_API
import me.proxer.library.ProxerException.ServerErrorType.USER
import me.proxer.library.ProxerException.ServerErrorType.USERINFO_INVALID_ID
import me.proxer.library.ProxerException.ServerErrorType.USER_2FA_SECRET_REQUIRED
import me.proxer.library.ProxerException.ServerErrorType.USER_ACCOUNT_BLOCKED
import me.proxer.library.ProxerException.ServerErrorType.USER_ACCOUNT_EXPIRED
import me.proxer.library.ProxerException.ServerErrorType.USER_INSUFFICIENT_PERMISSIONS
import me.proxer.library.ProxerException.ServerErrorType.WIKI_INVALID_PERMISSIONS
import me.proxer.library.ProxerException.ServerErrorType.WIKI_INVALID_TITLE
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl
import java.io.IOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLPeerUnverifiedException

/**
 * @author Ruben Gees
 */
object ErrorUtils {

    const val ENTRY_DATA_KEY = "entry"
    const val CHAPTER_TITLE_DATA_KEY = "chapterTitle"
    const val LINK_DATA_KEY = "link"

    private val apiErrors = arrayOf(
        UNKNOWN_API,
        API_REMOVED,
        INVALID_API_CLASS,
        INVALID_API_FUNCTION,
        INSUFFICIENT_PERMISSIONS,
        FUNCTION_BLOCKED,
        COMMENT_SAVE_ERROR
    )

    private val maintenanceErrors = arrayOf(SERVER_MAINTENANCE, API_MAINTENANCE)

    private val loginErrors = arrayOf(
        INVALID_TOKEN, NOTIFICATIONS_LOGIN_REQUIRED, UCP_LOGIN_REQUIRED, INFO_LOGIN_REQUIRED, MESSAGES_LOGIN_REQUIRED,
        USER_2FA_SECRET_REQUIRED, ANIME_LOGIN_REQUIRED, IP_AUTHENTICATION_REQUIRED,
        COMMENT_LOGIN_REQUIRED
    )

    private val clientErrors = arrayOf(
        NEWS, LOGIN_MISSING_CREDENTIALS, UCP_INVALID_CATEGORY, INFO_INVALID_TYPE, LOGIN_ALREADY_LOGGED_IN,
        LOGIN_DIFFERENT_USER_ALREADY_LOGGED_IN, LIST_INVALID_CATEGORY, LIST_INVALID_MEDIUM, MEDIA_INVALID_STYLE,
        ANIME_INVALID_STREAM, LIST_INVALID_LANGUAGE, LIST_INVALID_TYPE, ERRORLOG_INVALID_INPUT, LIST_INVALID_SUBJECT,
        CHAT_INVALID_ROOM, CHAT_INVALID_MESSAGE, CHAT_LOGIN_REQUIRED, CHAT_INVALID_THANK_YOU, CHAT_INVALID_INPUT,
        INFO_DELETE_COMMENT_INVALID_INPUT, UCP_INVALID_SETTINGS, COMMENT_INVALID_ID, COMMENT_INVALID_COMMENT,
        COMMENT_INVALID_RATING, COMMENT_INVALID_EPISODE, COMMENT_INVALID_STATUS, COMMENT_INVALID_ENTRY_ID,
        COMMENT_INVALID_CONTENT, COMMENT_ALREADY_EXISTS, WIKI_INVALID_TITLE
    )

    private val invalidIdErrors = arrayOf(
        USERINFO_INVALID_ID, UCP_INVALID_ID, INFO_INVALID_ID, MEDIA_INVALID_ENTRY, UCP_INVALID_EPISODE,
        MESSAGES_INVALID_CONFERENCE, LIST_INVALID_ID, FORUM_INVALID_ID, APPS_INVALID_ID
    )

    private val unsupportedErrors = arrayOf(
        USER, AUTH_INVALID_USER, AUTH_CODE_ALREADY_EXISTS, AUTH_CODE_DOES_NOT_EXIST, AUTH_CODE_REJECTED,
        AUTH_CODE_PENDING, AUTH_CODE_INVALID_NAME, AUTH_CODE_DUPLICATE, LIST_NO_ENTRIES_LEFT
    )

    private val storageHelper by safeInject<StorageHelper>()

    fun getMessage(error: Throwable): Int {
        return when (val innermostError = getInnermostError(error)) {
            is ProxerException -> getMessageForProxerException(innermostError)
            is HttpDataSource.InvalidResponseCodeException -> when (innermostError.responseCode) {
                404 -> R.string.error_video_deleted
                503 -> R.string.error_video_service_unavailable
                in 400 until 600 -> R.string.error_video_unknown
                else -> R.string.error_unknown
            }
            is SocketTimeoutException -> R.string.error_timeout
            is SSLPeerUnverifiedException -> R.string.error_ssl
            is NotConnectedException -> R.string.error_no_network
            is IOException -> R.string.error_io
            is NotLoggedInException -> R.string.error_login_required
            is AgeConfirmationRequiredException -> R.string.error_age_confirmation_needed
            is StreamResolutionException -> R.string.error_stream_resolution
            is MangaNotAvailableException -> R.string.error_manga_not_available
            is MangaLinkException -> R.string.error_manga_link
            is CommentTooLongException -> R.string.error_comment_too_long
            is CommentInvalidProgressException -> R.string.error_comment_invalid_progress
            else -> R.string.error_unknown
        }
    }

    fun isIpBlockedError(error: Throwable) = getInnermostError(error).let {
        it is ProxerException && it.serverErrorType == IP_BLOCKED
    }

    fun handle(error: Throwable): ErrorAction {
        val innermostError = getInnermostError(error)
        val errorMessage = getMessage(innermostError)

        val buttonMessage = when (innermostError) {
            is ProxerException -> when (innermostError.serverErrorType) {
                IP_BLOCKED -> R.string.error_action_captcha
                MEDIA_REMOVED_DUE_TO_COPYRIGHT -> ACTION_MESSAGE_HIDE
                in loginErrors -> R.string.error_action_login
                else -> ACTION_MESSAGE_DEFAULT
            }
            is NotLoggedInException -> R.string.error_action_login
            is NotConnectedException -> R.string.error_action_network_settings
            is AgeConfirmationRequiredException -> R.string.error_action_confirm
            is MangaLinkException -> R.string.error_action_open_link
            else -> ACTION_MESSAGE_DEFAULT
        }

        val buttonAction = when (innermostError) {
            is ProxerException -> when (innermostError.serverErrorType) {
                IP_BLOCKED -> CAPTCHA
                in loginErrors -> LOGIN
                else -> null
            }
            is NotLoggedInException -> LOGIN
            is NotConnectedException -> NETWORK_SETTINGS
            is AgeConfirmationRequiredException -> AGE_CONFIRMATION
            is MangaLinkException -> OPEN_LINK
            else -> null
        }

        val data = mutableMapOf<String, Any?>()

        if (error is PartialException) {
            data[ENTRY_DATA_KEY] = error.partialData
        }

        if (innermostError is MangaLinkException) {
            data[CHAPTER_TITLE_DATA_KEY] = innermostError.chapterTitle
            data[LINK_DATA_KEY] = innermostError.link
        }

        return ErrorAction(errorMessage, buttonMessage, buttonAction, data)
    }

    private fun getMessageForProxerException(error: ProxerException) = when (error.errorType) {
        SERVER -> when (error.serverErrorType) {
            IP_BLOCKED -> R.string.error_captcha
            RATE_LIMIT -> R.string.error_rate_limit
            INVALID_TOKEN -> R.string.error_invalid_token
            LOGIN_INVALID_CREDENTIALS -> R.string.error_login_credentials
            INFO_ENTRY_ALREADY_IN_LIST -> R.string.error_already_in_list
            INFO_EXCEEDED_MAXIMUM_ENTRIES -> R.string.error_list_full
            MANGA_INVALID_CHAPTER -> R.string.error_invalid_chapter
            ANIME_INVALID_EPISODE -> R.string.error_invalid_episode
            MESSAGES_INVALID_REPORT_INPUT -> R.string.error_invalid_input
            MESSAGES_INVALID_MESSAGE -> R.string.error_invalid_input
            MESSAGES_INVALID_USER -> R.string.error_cant_add_user_to_conference
            MESSAGES_MISSING_USER -> R.string.error_invalid_users_for_conference
            MESSAGES_EXCEEDED_MAXIMUM_USERS -> R.string.error_conference_full
            MESSAGES_INVALID_TOPIC -> R.string.error_invalid_topic
            LIST_TOP_ACCESS_RESET -> R.string.error_list_top_access_reset
            USER_2FA_SECRET_REQUIRED -> R.string.error_login_two_factor_authentication
            USER_ACCOUNT_EXPIRED -> R.string.error_account_expired
            USER_ACCOUNT_BLOCKED -> R.string.error_account_blocked
            USER_INSUFFICIENT_PERMISSIONS -> when (storageHelper.isLoggedIn) {
                true -> R.string.error_insufficient_permissions_logged_in
                false -> R.string.error_insufficient_permissions
            }
            CHAT_SEVEN_DAY_PROTECTION -> R.string.error_chat_seven_days
            CHAT_USER_ON_BLACKLIST -> R.string.error_chat_blacklist
            CHAT_INVALID_PERMISSIONS, CHAT_NO_PERMISSIONS -> R.string.error_chat_no_permissions
            FORUM_INVALID_PERMISSIONS -> R.string.error_forum_no_permissions
            COMMENT_INSUFFICIENT_PERMISSIONS -> R.string.error_comment_no_permissions
            WIKI_INVALID_PERMISSIONS -> R.string.error_wiki_no_permissions
            COMMENT_NOT_ACTIVE_YET -> R.string.error_comment_not_active_yet
            IP_AUTHENTICATION_REQUIRED -> R.string.error_login_ip_authentication
            MEDIA_REMOVED_DUE_TO_COPYRIGHT -> R.string.error_media_removed_due_to_copyright
            INTERNAL -> R.string.error_internal
            in apiErrors -> R.string.error_api
            in maintenanceErrors -> R.string.error_maintenance
            in loginErrors -> R.string.error_login
            in clientErrors -> R.string.error_client
            in invalidIdErrors -> R.string.error_invalid_id
            in unsupportedErrors -> R.string.error_unsupported_code
            else -> R.string.error_unknown
        }
        IO -> R.string.error_io
        TIMEOUT -> R.string.error_timeout
        PARSING -> R.string.error_parsing
        UNKNOWN, CANCELLED -> R.string.error_unknown
    }

    private fun getInnermostError(error: Throwable): Throwable = when (error) {
        is ProxerException -> when (error.errorType) {
            IO -> error.cause?.let { getInnermostError(it) } ?: error
            UNKNOWN -> error.cause?.let { getInnermostError(it) } ?: error
            else -> error
        }
        is PartialException -> error.innerError
        is ChatException -> error.innerError
        is ExoPlaybackException -> error.cause?.let { getInnermostError(it) } ?: error
        is Loader.UnexpectedLoaderException -> error.cause?.let { getInnermostError(it) } ?: error
        else -> error
    }

    open class ErrorAction(
        val message: Int,
        val buttonMessage: Int = ACTION_MESSAGE_DEFAULT,
        val buttonAction: ButtonAction? = null,
        val data: Map<String, Any?> = emptyMap()
    ) {

        companion object {
            const val ACTION_MESSAGE_DEFAULT = -1
            const val ACTION_MESSAGE_HIDE = -2
        }

        fun toClickListener(activity: BaseActivity) = when (buttonAction) {
            CAPTCHA -> View.OnClickListener {
                activity.showPage(ProxerUrls.captchaWeb(Utils.getIpAddress(), Device.MOBILE), skipCheck = true)
            }
            NETWORK_SETTINGS -> View.OnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    activity.startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
                } else {
                    activity.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                }
            }
            LOGIN -> View.OnClickListener { LoginDialog.show(activity) }
            AGE_CONFIRMATION -> View.OnClickListener { AgeConfirmationDialog.show(activity) }
            OPEN_LINK -> data[LINK_DATA_KEY].let { link ->
                when (link) {
                    is HttpUrl -> View.OnClickListener { activity.showPage(link, skipCheck = true) }
                    else -> null
                }
            }
            else -> null
        }

        fun toIntent() = when (buttonAction) {
            CAPTCHA -> Intent(
                Intent.ACTION_VIEW,
                ProxerUrls.captchaWeb(Utils.getIpAddress(), Device.MOBILE).androidUri()
            )
            else -> null
        }

        enum class ButtonAction { CAPTCHA, NETWORK_SETTINGS, LOGIN, AGE_CONFIRMATION, OPEN_LINK, BOOKMARK }
    }
}
