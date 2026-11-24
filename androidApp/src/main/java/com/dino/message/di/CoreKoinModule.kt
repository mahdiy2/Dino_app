package com.dino.message.di

import androidx.room.Room
import com.dino.message.corefeature.data.upload.AndroidMediaOutbox
import com.dino.message.corefeature.data.upload.ApiMediaSubmissionExecutor
import com.dino.message.corefeature.data.upload.UploadedAttachmentTransport
import com.dino.message.corefeature.data.upload.WorkManagerMediaSubmissionScheduler
import com.dino.message.corefeature.data.upload.mediaReplayCapability
import io.ktor.client.HttpClient
import org.json.JSONObject
import com.dino.message.corefeature.data.upload.MediaSubmissionDatabase
import com.dino.message.corefeature.data.upload.MediaSubmissionExecutor
import java.io.File
import com.dino.message.DinoOrderApplication
import com.dino.message.chatfeature.data.notification.AndroidMessageDeduplicationStore
import com.dino.message.chatfeature.data.notification.AndroidMessageSoundPlayer
import com.dino.message.chatfeature.data.notification.ActiveChatTracker
import com.dino.message.chatfeature.data.notification.ActiveConversationTrackerCleaner
import com.dino.message.chatfeature.data.notification.ActiveGroupTracker
import com.dino.message.chatfeature.data.notification.InAppNotificationManager
import com.dino.message.chatfeature.data.notification.NotificationHelper
import com.dino.message.chatfeature.domain.audio.MessageSoundPlayer
import com.dino.message.chatfeature.presentation.component.AndroidAudioRecorderFactory
import com.dino.message.chatfeature.presentation.component.PlatformAudioRecorderFactory
import com.dino.message.corefeature.data.background.AppLifecycleBackgroundTaskScheduler
import com.dino.message.corefeature.data.config.AndroidAppConfig
import com.dino.message.corefeature.data.coroutine.AndroidAppDispatchers
import com.dino.message.corefeature.data.crypto.DefaultMessageCrypto
import com.dino.message.corefeature.data.crypto.JvmAesGcmCrypto
import com.dino.message.corefeature.data.crypto.JvmBase64Codec
import com.dino.message.corefeature.data.crypto.JvmHashCrypto
import com.dino.message.corefeature.data.crypto.JvmRsaCrypto
import com.dino.message.corefeature.data.db.MessageDatabase
import com.dino.message.corefeature.data.db.buildMessageDatabase
import com.dino.message.corefeature.data.filesystem.AndroidFileSystemProvider
import com.dino.message.corefeature.data.network.AndroidNetworkStatusProvider
import com.dino.message.corefeature.data.notification.AndroidAppNotificationController
import com.dino.message.corefeature.data.notification.AndroidNotificationDedupeSnapshotStore
import com.dino.message.corefeature.data.notification.SPrefNotificationTokenProvider
import com.dino.message.corefeature.data.remote.AndroidNetworkErrorMessageProvider
import com.dino.message.corefeature.data.remote.NetworkExceptionClassifier
import com.dino.message.corefeature.data.remote.JvmNetworkExceptionClassifier
import com.dino.message.corefeature.data.remote.NetworkIssueReporter
import com.dino.message.corefeature.data.remote.util.NetworkErrorMessageProvider
import com.dino.message.corefeature.data.remote.util.HttpAPIUtil
import com.dino.message.corefeature.data.remote.createAppHttpClient
import com.dino.message.corefeature.data.remote.logging.AndroidAppLogger
import com.dino.message.corefeature.data.resource.AndroidStringResolver
import com.dino.message.corefeature.data.session.RoomLocalMessageStoreCleaner
import com.dino.message.corefeature.data.session.SPrefLocalSessionStore
import com.dino.message.corefeature.data.upload.AndroidMediaMetadataExtractor
import com.dino.message.corefeature.data.upload.AndroidUploadContentProvider
import com.dino.message.corefeature.domain.upload.PlatformMediaMetadataExtractor
import com.dino.message.corefeature.domain.upload.PlatformMediaResolver
import com.dino.message.corefeature.data.session.SPrefSessionProvider
import com.dino.message.corefeature.data.socket.AndroidSocketIoTransportFactory
import com.dino.message.corefeature.data.socket.AppSocketManager
import com.dino.message.corefeature.domain.socket.RealtimeSocketGateway
import com.dino.message.corefeature.data.socket.RealtimeSocketTransportFactory
import com.dino.message.corefeature.data.socket.RoomSocketOutboundQueueStore
import com.dino.message.corefeature.data.socket.SocketOutboundQueueStore
import com.dino.message.corefeature.data.spref.AndroidEncryptedSecureKeyValueStore
import com.dino.message.corefeature.data.spref.DataStoreUtil
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.data.spref.SecureKeyValueStore
import com.dino.message.corefeature.data.spref.createSettingsDataStore
import com.dino.message.corefeature.data.system.AndroidPlatformInfoProvider
import com.dino.message.corefeature.data.system.AndroidProcessController
import com.dino.message.corefeature.data.system.SystemAppClock
import com.dino.message.corefeature.data.util.DeviceIdProvider
import com.dino.message.corefeature.domain.background.BackgroundTaskRegistry
import com.dino.message.corefeature.domain.background.BackgroundTaskScheduler
import com.dino.message.corefeature.domain.config.AppConfig
import com.dino.message.corefeature.domain.coroutine.AppDispatchers
import com.dino.message.corefeature.domain.crypto.AesGcmCrypto
import com.dino.message.corefeature.domain.crypto.Base64Codec
import com.dino.message.corefeature.domain.crypto.HashCrypto
import com.dino.message.corefeature.domain.crypto.MessageCrypto
import com.dino.message.corefeature.domain.crypto.RsaCrypto
import com.dino.message.corefeature.domain.device.DeviceInfoProvider
import com.dino.message.corefeature.domain.filesystem.FileSystemProvider
import com.dino.message.corefeature.domain.lifecycle.AppBackgroundCleanupObserver
import com.dino.message.corefeature.domain.lifecycle.AppLifecycleDispatcher
import com.dino.message.corefeature.domain.logging.AppLogger
import com.dino.message.corefeature.domain.network.NetworkStatusProvider
import com.dino.message.corefeature.domain.notification.AppNotificationController
import com.dino.message.corefeature.domain.notification.AppNotificationPresenter
import com.dino.message.corefeature.domain.notification.NotificationDedupeSnapshotStore
import com.dino.message.corefeature.domain.notification.DefaultNotificationSocketController
import com.dino.message.corefeature.domain.notification.NotificationSocketConnection
import com.dino.message.corefeature.domain.notification.NotificationSocketController
import com.dino.message.corefeature.domain.notification.NotificationTokenProvider
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.domain.session.ActiveChatState
import com.dino.message.corefeature.domain.session.ActiveConversationStateCleaner
import com.dino.message.corefeature.domain.session.ActiveGroupState
import com.dino.message.corefeature.domain.session.AuthSessionStore
import com.dino.message.corefeature.domain.session.LocalMessageStoreCleaner
import com.dino.message.corefeature.domain.session.LocalSessionCleaner
import com.dino.message.corefeature.domain.session.LocalSessionStore
import com.dino.message.corefeature.domain.session.SessionProvider
import com.dino.message.corefeature.domain.settings.AppSettingsStore
import com.dino.message.corefeature.domain.startup.AppStartupCoordinator
import com.dino.message.corefeature.domain.system.AppClock
import com.dino.message.corefeature.domain.system.AppProcessController
import com.dino.message.corefeature.domain.system.PlatformInfoProvider
import com.dino.message.corefeature.presentation.navigation.AndroidIntentDeepLinkHandler
import com.dino.message.corefeature.presentation.activity.MainActivityUiSettingsState
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.chatfeature.domain.util.MessageDeduplicationStore
import com.dino.message.chatfeature.domain.util.MessageDeduplicator
import com.dino.message.crashfeature.data.remote.CrashLogApiService
import com.dino.message.crashfeature.data.service.AndroidCrashUiLauncher
import com.dino.message.crashfeature.data.service.CrashLogBuilder
import com.dino.message.crashfeature.data.service.CrashReporter
import com.dino.message.crashfeature.data.service.CrashUiLauncher
import com.dino.message.profilefeature.data.share.AndroidReferralShareLauncher
import com.dino.message.profilefeature.domain.share.ReferralShareLauncher
import com.dino.message.threadfeature.presentation.music.MediaPlaybackController
import com.dino.message.threadfeature.presentation.music.MusicPlaybackManager
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single<DinoOrderApplication> { androidApplication() as DinoOrderApplication }
    single<AppConfig> { AndroidAppConfig }
    single<AppDispatchers> { AndroidAppDispatchers }
    single<AppClock> { SystemAppClock() }
    single<AppProcessController> { AndroidProcessController() }
    single<PlatformInfoProvider> { AndroidPlatformInfoProvider() }
    single<DeviceInfoProvider> { DeviceIdProvider(context = androidContext()) }
    single<StringResolver> { AndroidStringResolver(context = androidContext()) }
    single<NetworkStatusProvider> { AndroidNetworkStatusProvider(context = androidContext()) }
    single<FileSystemProvider> { AndroidFileSystemProvider(context = androidContext()) }
    single<PlatformAudioRecorderFactory> { AndroidAudioRecorderFactory(context = androidContext()) }
    single<MediaPlaybackController> { MusicPlaybackManager }
    single<Base64Codec> { JvmBase64Codec() }
    single<AesGcmCrypto> { JvmAesGcmCrypto() }
    single<HashCrypto> { JvmHashCrypto() }
    single<RsaCrypto> { JvmRsaCrypto(base64Codec = get()) }
    single<MessageCrypto> {
        DefaultMessageCrypto(
            base64Codec = get(),
            rsaCrypto = get(),
            aesGcmCrypto = get(),
            hashCrypto = get(),
        )
    }
    single { UiEventBus() }
    single { AppLifecycleDispatcher() }
    single<AppNotificationPresenter> { NotificationHelper(context = androidContext()) }
    single<NotificationDedupeSnapshotStore> {
        AndroidNotificationDedupeSnapshotStore(context = androidContext())
    }
    single<MessageDeduplicationStore> {
        AndroidMessageDeduplicationStore(context = androidContext())
    }
    factory { MessageDeduplicator(store = get()) }
    single<MessageSoundPlayer> { AndroidMessageSoundPlayer(context = androidContext()) }
    single<ReferralShareLauncher> { AndroidReferralShareLauncher(context = androidContext()) }
    single<AppNotificationController> {
        AndroidAppNotificationController(
            notificationPresenter = get(),
            inAppNotificationManager = get(),
        )
    }
    single<NotificationTokenProvider> { SPrefNotificationTokenProvider(sPrefManager = get()) }
    single<NotificationSocketConnection> { get<InAppNotificationManager>() }
    single<NotificationSocketController> {
        DefaultNotificationSocketController(
            tokenProvider = get(),
            socketConnection = get(),
        )
    }
    single { AndroidIntentDeepLinkHandler() }
    single<SecureKeyValueStore> { AndroidEncryptedSecureKeyValueStore(context = androidContext()) }
    single { SPrefManager(secureStore = get()) }
    single<AuthSessionStore> { get<SPrefManager>() }
    single<SessionProvider> { SPrefSessionProvider(sPrefManager = get()) }
    single { SPrefLocalSessionStore(sPrefManager = get()) }
    single<LocalSessionStore> { get<SPrefLocalSessionStore>() }
    single<LocalSessionCleaner> { get<SPrefLocalSessionStore>() }
    single<LocalMessageStoreCleaner> { RoomLocalMessageStoreCleaner(messageDatabase = get()) }
    single<ActiveConversationStateCleaner> { ActiveConversationTrackerCleaner() }
    single<ActiveChatState> { ActiveChatTracker }
    single<ActiveGroupState> { ActiveGroupTracker }
    single<AppLogger> { AndroidAppLogger }
    single<NetworkExceptionClassifier> { JvmNetworkExceptionClassifier }
    single<NetworkErrorMessageProvider> { AndroidNetworkErrorMessageProvider() }
    single { AndroidUploadContentProvider(contentResolver = androidContext().contentResolver) }
    single<PlatformMediaResolver> { get<AndroidUploadContentProvider>() }
    single<PlatformMediaMetadataExtractor> {
        AndroidMediaMetadataExtractor(contentResolver = androidContext().contentResolver)
    }
    single {
        createAppHttpClient(
            engineFactory = OkHttp,
            baseUrl = get<AppConfig>().baseUrl,
            isDebug = get<AppConfig>().isDebug,
            appLogger = get(),
        )
    }
    single {
        HttpAPIUtil(
            sPrefManager = get(),
            networkExceptionClassifier = get(),
            appLogger = get(),
            uiEventBus = get(),
            errorMessageProvider = get(),
            appConfig = get(),
        )
    }
    single { createSettingsDataStore(context = androidContext()) }
    single { DataStoreUtil(dataStore = get()) }
    single<AppSettingsStore> { get<DataStoreUtil>() }
    single { MainActivityUiSettingsState(appSettingsStore = get()) }
    single { CrashLogApiService(httpClient = get(), sPrefManager = get()) }
    single { CrashLogBuilder(appConfig = get(), platformInfoProvider = get()) }
    single<CrashUiLauncher> {
        AndroidCrashUiLauncher(
            context = androidContext(),
            appLogger = get(),
        )
    }
    single {
        Room.databaseBuilder(
            androidContext(), MediaSubmissionDatabase::class.java,
            File(androidContext().noBackupFilesDir, "media-submissions.db").absolutePath,
        ).build()
    }
    single<MediaSubmissionExecutor> { ApiMediaSubmissionExecutor(get(), get(), get(), get()) }
    single {
        val http = get<HttpClient>()
        val socket = get<AppSocketManager>()
        AndroidMediaOutbox(
            files = File(androidContext().noBackupFilesDir, "pending-media"),
            dao = get<MediaSubmissionDatabase>().submissions(),
            session = get(),
            replayCapability = { http.mediaReplayCapability() },
            executor = get(),
            socket = UploadedAttachmentTransport { owner, sessionId, event, payload, key ->
                socket.enqueueUploadedAttachment(owner, sessionId, event, JSONObject(payload), key)
            },
            work = WorkManagerMediaSubmissionScheduler(androidContext()),
        )
    }
    single { AppLifecycleBackgroundTaskScheduler(androidContext()) }
    single<BackgroundTaskScheduler> { get<AppLifecycleBackgroundTaskScheduler>() }
    single<BackgroundTaskRegistry> { get<AppLifecycleBackgroundTaskScheduler>() }
    single<RealtimeSocketTransportFactory> {
        AndroidSocketIoTransportFactory(
            baseUrl = get<AppConfig>().baseUrl,
            appLogger = get(),
        )
    }
    single<SocketOutboundQueueStore> { RoomSocketOutboundQueueStore(socketOutboundQueueDao = get()) }
    single {
        CrashReporter(
            crashLogDao = get(),
            crashLogApiService = get(),
            sPrefManager = get(),
            backgroundTaskScheduler = get(),
            appClock = get(),
            appLogger = get(),
            appProcessController = get(),
            crashLogBuilder = get(),
            crashUiLauncher = get(),
        )
    }
    single<NetworkIssueReporter> { get<CrashReporter>() }
    single {
        AppSocketManager(
            sprefManager = get(),
            crashReporter = get(),
            socketOutboundQueueStore = get(),
            transportFactory = get(),
            backgroundTaskScheduler = get(),
            appClock = get(),
            appLogger = get(),
            backgroundTaskRegistry = get(),
        )
    }
    single<RealtimeSocketGateway> { get<AppSocketManager>() }
    single {
        InAppNotificationManager(
            appSocketManager = get(),
            notificationPresenter = get(),
            dedupeSnapshotStore = get(),
            appClock = get(),
            appLogger = get(),
        )
    }
    single {
        AppBackgroundCleanupObserver(
            notificationSocketController = get(),
            activeConversationStateCleaner = get(),
        )
    }
    single {
        AppStartupCoordinator(
            lifecycleDispatcher = get(),
            backgroundCleanupObserver = get<AppBackgroundCleanupObserver>(),
        )
    }
}

val databaseModule = module {
    single { buildMessageDatabase(androidContext()) }
    single { get<MessageDatabase>().messageDao() }
    single { get<MessageDatabase>().groupMessageDao() }
    single { get<MessageDatabase>().crashLogDao() }
    single { get<MessageDatabase>().socketOutboundQueueDao() }
}
