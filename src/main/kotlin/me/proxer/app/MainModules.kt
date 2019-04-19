package me.proxer.app

import android.content.Context
import android.content.res.Resources
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.gojuno.koptional.Optional
import com.rubengees.rxbus.RxBus
import com.squareup.moshi.Moshi
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.anime.AnimeViewModel
import me.proxer.app.anime.schedule.ScheduleViewModel
import me.proxer.app.auth.LoginHandler
import me.proxer.app.auth.LoginViewModel
import me.proxer.app.auth.LogoutViewModel
import me.proxer.app.auth.ProxerLoginTokenManager
import me.proxer.app.bookmark.BookmarkViewModel
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.conference.ConferenceViewModel
import me.proxer.app.chat.prv.conference.info.ConferenceInfoViewModel
import me.proxer.app.chat.prv.create.CreateConferenceViewModel
import me.proxer.app.chat.prv.message.MessengerViewModel
import me.proxer.app.chat.prv.sync.MessengerDatabase
import me.proxer.app.chat.pub.message.ChatReportViewModel
import me.proxer.app.chat.pub.message.ChatViewModel
import me.proxer.app.chat.pub.room.ChatRoomViewModel
import me.proxer.app.chat.pub.room.info.ChatRoomInfoViewModel
import me.proxer.app.forum.TopicViewModel
import me.proxer.app.info.industry.IndustryInfoViewModel
import me.proxer.app.info.industry.IndustryProjectViewModel
import me.proxer.app.info.translatorgroup.TranslatorGroupInfoViewModel
import me.proxer.app.info.translatorgroup.TranslatorGroupProjectViewModel
import me.proxer.app.manga.MangaViewModel
import me.proxer.app.media.MediaInfoViewModel
import me.proxer.app.media.TagDatabase
import me.proxer.app.media.comment.CommentViewModel
import me.proxer.app.media.discussion.DiscussionViewModel
import me.proxer.app.media.episode.EpisodeViewModel
import me.proxer.app.media.list.MediaListViewModel
import me.proxer.app.media.recommendation.RecommendationViewModel
import me.proxer.app.media.relation.RelationViewModel
import me.proxer.app.news.NewsViewModel
import me.proxer.app.notification.NotificationViewModel
import me.proxer.app.profile.ProfileViewModel
import me.proxer.app.profile.about.ProfileAboutViewModel
import me.proxer.app.profile.comment.ProfileCommentViewModel
import me.proxer.app.profile.history.HistoryViewModel
import me.proxer.app.profile.media.ProfileMediaListViewModel
import me.proxer.app.profile.topten.TopTenViewModel
import me.proxer.app.settings.status.ServerStatusViewModel
import me.proxer.app.ucp.history.UcpHistoryViewModel
import me.proxer.app.ucp.media.UcpMediaListViewModel
import me.proxer.app.ucp.overview.UcpOverviewViewModel
import me.proxer.app.ucp.settings.UcpSettingsViewModel
import me.proxer.app.ucp.topten.UcpTopTenViewModel
import me.proxer.app.util.Validators
import me.proxer.app.util.data.HawkMoshiParser
import me.proxer.app.util.data.LocalDataInitializer
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.http.CacheInterceptor
import me.proxer.app.util.http.ConnectionCloseInterceptor
import me.proxer.app.util.http.HttpsUpgradeInterceptor
import me.proxer.app.util.http.TaggedSocketFactory
import me.proxer.app.util.http.UserAgentInterceptor
import me.proxer.library.LoginTokenManager
import me.proxer.library.ProxerApi
import me.proxer.library.enums.AnimeLanguage
import me.proxer.library.enums.Category
import me.proxer.library.enums.CommentSortCriteria
import me.proxer.library.enums.Language
import me.proxer.library.enums.UserMediaListFilterType
import okhttp3.Cache
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.bind
import org.koin.dsl.module
import timber.log.Timber
import java.io.File
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

const val DEFAULT_RX_PREFERENCES = "defaultRxPreferences"
const val HAWK_RX_PREFERENCES = "hawkRxPreferences"

private const val CHAT_DATABASE_NAME = "chat.db"
private const val TAG_DATABASE_NAME = "tag.db"
private const val HAWK_PREFERENCE_NAME = "Hawk2"

private const val HTTP_CACHE_SIZE = 1_024L * 1_024L * 10L
private const val HTTP_CACHE_NAME = "http"

private const val API_TOKEN_HEADER = "proxer-api-token"

private val headersToRedact = listOf("proxer-api-key", "set-cookie")

private val applicationModules = module(createdAtStart = true) {
    single { PreferenceManager.getDefaultSharedPreferences(androidContext()) }
    single { androidContext().packageManager }

    single(StringQualifier(DEFAULT_RX_PREFERENCES)) { RxSharedPreferences.create(get()) }
    single(StringQualifier(HAWK_RX_PREFERENCES)) {
        RxSharedPreferences.create(androidContext().getSharedPreferences(HAWK_PREFERENCE_NAME, Context.MODE_PRIVATE))
    }

    single { StorageHelper(get(), get(StringQualifier(HAWK_RX_PREFERENCES))) }
    single { PreferenceHelper(get(), get(StringQualifier(DEFAULT_RX_PREFERENCES)), get()) }

    single { RxBus() }

    single {
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(null as KeyStore?)
        }

        trustManagerFactory.trustManagers.filterIsInstance(X509TrustManager::class.java).first()
    }

    single {
        val preferenceHelper = get<PreferenceHelper>()

        val loggingInterceptor = when {
            BuildConfig.LOG -> HttpLoggingInterceptor { message -> Timber.i(message) }.apply {
                level = preferenceHelper.httpLogLevel

                headersToRedact.forEach { redactHeader(it) }

                if (preferenceHelper.shouldRedactToken) {
                    redactHeader(API_TOKEN_HEADER)
                }
            }
            else -> null
        }

        OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
            .socketFactory(TaggedSocketFactory())
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addNetworkInterceptor(CacheInterceptor())
            .addInterceptor(HttpsUpgradeInterceptor())
            .addInterceptor(UserAgentInterceptor())
            .addInterceptor(ConnectionCloseInterceptor())
            .cache(Cache(File(androidContext().cacheDir, HTTP_CACHE_NAME), HTTP_CACHE_SIZE))
            .apply {
                if (loggingInterceptor != null) {
                    if (preferenceHelper.shouldLogHttpVerbose) {
                        addNetworkInterceptor(loggingInterceptor)
                    } else {
                        addInterceptor(loggingInterceptor)
                    }
                }
            }
            .build()
    }

    single { Moshi.Builder().build() }

    single {
        ProxerApi.Builder(BuildConfig.PROXER_API_KEY)
            .userAgent(USER_AGENT)
            .loginTokenManager(get())
            .client(get())
            .moshi(get())
            .build()
    }

    single { Validators(get(), get()) }

    single { Room.databaseBuilder(androidContext(), MessengerDatabase::class.java, CHAT_DATABASE_NAME).build() }
    single { Room.databaseBuilder(androidContext(), TagDatabase::class.java, TAG_DATABASE_NAME).build() }
    single { get<MessengerDatabase>().dao() }
    single { get<TagDatabase>().dao() }

    single { HawkMoshiParser(get()) }
    single { LocalDataInitializer(androidContext(), get()) }

    single { ProxerLoginTokenManager(get()) } bind LoginTokenManager::class
    single { LoginHandler(get(), get(), get()) }
}

private val viewModelModule = module {
    viewModel { LoginViewModel() }
    viewModel { LogoutViewModel() }

    viewModel { NewsViewModel() }
    viewModel { NotificationViewModel() }

    viewModel { CreateConferenceViewModel() }
    viewModel { (searchQuery: String) -> ConferenceViewModel(searchQuery) }
    viewModel { (conferenceId: String) -> ConferenceInfoViewModel(conferenceId) }
    viewModel { (initialConference: LocalConference) -> MessengerViewModel(initialConference) }
    viewModel { ChatRoomViewModel() }
    viewModel { ChatReportViewModel() }
    viewModel { (chatRoomId: String) -> ChatViewModel(chatRoomId) }
    viewModel { (chatRoomId: String) -> ChatRoomInfoViewModel(chatRoomId) }

    viewModel { (category: Category, filterAvailable: Boolean) -> BookmarkViewModel(category, filterAvailable) }

    viewModel { parameterList ->
        MediaListViewModel(
            parameterList[0], parameterList[1], parameterList[2], parameterList[3], parameterList[4],
            parameterList[5], parameterList[6], parameterList[7], parameterList[8], parameterList[9],
            parameterList[10], parameterList[11]
        )
    }

    viewModel { ScheduleViewModel() }

    viewModel { UcpHistoryViewModel() }
    viewModel { UcpOverviewViewModel() }
    viewModel { UcpTopTenViewModel() }
    viewModel { UcpSettingsViewModel() }
    viewModel { (userId: Optional<String>, username: Optional<String>) -> ProfileAboutViewModel(userId, username) }
    viewModel { (userId: Optional<String>, username: Optional<String>) -> ProfileViewModel(userId, username) }
    viewModel { (userId: Optional<String>, username: Optional<String>) -> TopTenViewModel(userId, username) }
    viewModel { (userId: Optional<String>, username: Optional<String>) -> HistoryViewModel(userId, username) }
    viewModel { (category: Category, filter: Optional<UserMediaListFilterType>) ->
        UcpMediaListViewModel(category, filter)
    }

    viewModel { parameterList ->
        ProfileMediaListViewModel(parameterList[0], parameterList[1], parameterList[2], parameterList[3])
    }

    viewModel { (userId: Optional<String>, username: Optional<String>, category: Optional<Category>) ->
        ProfileCommentViewModel(userId, username, category)
    }

    viewModel { (id: String, resources: Resources) -> TopicViewModel(id, resources) }

    viewModel { (entryId: String) -> MediaInfoViewModel(entryId) }
    viewModel { (entryId: String) -> EpisodeViewModel(entryId) }
    viewModel { (entryId: String, sortCriteria: CommentSortCriteria) -> CommentViewModel(entryId, sortCriteria) }
    viewModel { (entryId: String) -> RelationViewModel(entryId) }
    viewModel { (entryId: String) -> RecommendationViewModel(entryId) }
    viewModel { (entryId: String) -> DiscussionViewModel(entryId) }

    viewModel { (industryId: String) -> IndustryInfoViewModel(industryId) }
    viewModel { (industryId: String) -> IndustryProjectViewModel(industryId) }
    viewModel { (translatorGroupId: String) -> TranslatorGroupInfoViewModel(translatorGroupId) }
    viewModel { (translatorGroupId: String) -> TranslatorGroupProjectViewModel(translatorGroupId) }

    viewModel { (entryId: String, language: Language, episode: Int) -> MangaViewModel(entryId, language, episode) }
    viewModel { (entryId: String, language: AnimeLanguage, episode: Int) -> AnimeViewModel(entryId, language, episode) }

    viewModel { ServerStatusViewModel() }
}

val koinModules = listOf(applicationModules, viewModelModule)
