package com.dino.message.di

import com.dino.message.DinoOrderApplication
import com.dino.message.chatfeature.data.notification.InAppNotificationManager
import com.dino.message.corefeature.data.background.AppLifecycleBackgroundTaskScheduler
import com.dino.message.corefeature.data.config.AndroidAppConfig
import com.dino.message.corefeature.data.db.MessageDatabase
import com.dino.message.corefeature.data.db.buildMessageDatabase
import com.dino.message.corefeature.data.filesystem.AndroidFileSystemProvider
import com.dino.message.corefeature.data.network.AndroidNetworkStatusProvider
import com.dino.message.corefeature.data.notification.AndroidAppNotificationController
import com.dino.message.corefeature.data.remote.NetworkExceptionClassifier
import com.dino.message.corefeature.data.remote.JvmNetworkExceptionClassifier
import com.dino.message.corefeature.data.remote.util.HttpAPIUtil
import com.dino.message.corefeature.data.remote.createAppHttpClient
import com.dino.message.corefeature.data.remote.logging.AppLogger
import com.dino.message.corefeature.data.remote.logging.AndroidAppLogger
import com.dino.message.corefeature.data.resource.AndroidStringResolver
import com.dino.message.corefeature.data.upload.AndroidUploadContentProvider
import com.dino.message.corefeature.data.session.SPrefSessionProvider
import com.dino.message.corefeature.data.socket.AndroidSocketIoTransportFactory
import com.dino.message.corefeature.data.socket.AppSocketManager
import com.dino.message.corefeature.data.socket.RealtimeSocketGateway
import com.dino.message.corefeature.data.socket.RealtimeSocketTransportFactory
import com.dino.message.corefeature.data.socket.RoomSocketOutboundQueueStore
import com.dino.message.corefeature.data.socket.SocketOutboundQueueStore
import com.dino.message.corefeature.data.spref.AndroidEncryptedSecureKeyValueStore
import com.dino.message.corefeature.data.spref.DataStoreUtil
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.data.spref.SecureKeyValueStore
import com.dino.message.corefeature.data.spref.createSettingsDataStore
import com.dino.message.corefeature.domain.background.BackgroundTaskRegistry
import com.dino.message.corefeature.domain.background.BackgroundTaskScheduler
import com.dino.message.corefeature.domain.config.AppConfig
import com.dino.message.corefeature.domain.filesystem.FileSystemProvider
import com.dino.message.corefeature.domain.lifecycle.AppBackgroundCleanupObserver
import com.dino.message.corefeature.domain.lifecycle.AppLifecycleDispatcher
import com.dino.message.corefeature.domain.network.NetworkStatusProvider
import com.dino.message.corefeature.domain.notification.AppNotificationController
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.domain.session.SessionProvider
import com.dino.message.corefeature.presentation.navigation.AndroidIntentDeepLinkHandler
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.crashfeature.data.remote.CrashLogApiService
import com.dino.message.crashfeature.data.service.CrashReporter
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single<DinoOrderApplication> { androidApplication() as DinoOrderApplication }
    single<AppConfig> { AndroidAppConfig }
    single<StringResolver> { AndroidStringResolver(context = androidContext()) }
    single<NetworkStatusProvider> { AndroidNetworkStatusProvider(context = androidContext()) }
    single<FileSystemProvider> { AndroidFileSystemProvider(context = androidContext()) }
    single { UiEventBus() }
    single { AppLifecycleDispatcher() }
    single<AppNotificationController> {
        AndroidAppNotificationController(
            context = androidContext(),
            inAppNotificationManager = get(),
        )
    }
    single { AndroidIntentDeepLinkHandler() }
    single<SecureKeyValueStore> { AndroidEncryptedSecureKeyValueStore(context = androidContext()) }
    single { SPrefManager(secureStore = get()) }
    single<SessionProvider> { SPrefSessionProvider(sPrefManager = get()) }
    single<AppLogger> { AndroidAppLogger }
    single<NetworkExceptionClassifier> { JvmNetworkExceptionClassifier }
    single { AndroidUploadContentProvider(contentResolver = androidContext().contentResolver) }
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
            context = androidContext(),
            networkExceptionClassifier = get(),
            appLogger = get(),
            uiEventBus = get(),
            stringResolver = get(),
            appConfig = get(),
        )
    }
    single { createSettingsDataStore(context = androidContext()) }
    single { DataStoreUtil(dataStore = get()) }
    single { CrashLogApiService(httpClient = get(), sPrefManager = get()) }
    single { AppLifecycleBackgroundTaskScheduler() }
    single<BackgroundTaskScheduler> { get<AppLifecycleBackgroundTaskScheduler>() }
    single<BackgroundTaskRegistry> { get<AppLifecycleBackgroundTaskScheduler>() }
    single<RealtimeSocketTransportFactory> { AndroidSocketIoTransportFactory(baseUrl = get<AppConfig>().baseUrl) }
    single<SocketOutboundQueueStore> { RoomSocketOutboundQueueStore(socketOutboundQueueDao = get()) }
    single {
        CrashReporter(
            context = androidContext(),
            crashLogDao = get(),
            crashLogApiService = get(),
            sPrefManager = get(),
            backgroundTaskScheduler = get(),
            backgroundTaskRegistry = get(),
        )
    }
    single {
        AppSocketManager(
            sprefManager = get(),
            crashReporter = get(),
            socketOutboundQueueStore = get(),
            transportFactory = get(),
            backgroundTaskScheduler = get(),
            backgroundTaskRegistry = get(),
        )
    }
    single<RealtimeSocketGateway> { get<AppSocketManager>() }
    single {
        InAppNotificationManager(
            context = androidContext(),
            appSocketManager = get(),
        )
    }
    single { AppBackgroundCleanupObserver(inAppNotificationManager = get()) }
}

val databaseModule = module {
    single { buildMessageDatabase(androidContext()) }
    single { get<MessageDatabase>().messageDao() }
    single { get<MessageDatabase>().groupMessageDao() }
    single { get<MessageDatabase>().crashLogDao() }
    single { get<MessageDatabase>().socketOutboundQueueDao() }
}
