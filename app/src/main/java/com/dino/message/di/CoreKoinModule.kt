package com.dino.message.di

import com.dino.message.BuildConfig
import com.dino.message.DinoOrderApplication
import com.dino.message.chatfeature.data.notification.InAppNotificationManager
import com.dino.message.corefeature.data.db.MessageDatabase
import com.dino.message.corefeature.data.db.buildMessageDatabase
import com.dino.message.corefeature.data.remote.NetworkExceptionClassifier
import com.dino.message.corefeature.data.remote.JvmNetworkExceptionClassifier
import com.dino.message.corefeature.data.remote.util.HttpAPIUtil
import com.dino.message.corefeature.data.remote.createAppHttpClient
import com.dino.message.corefeature.data.remote.logging.AppLogger
import com.dino.message.corefeature.data.remote.logging.AndroidAppLogger
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
import com.dino.message.corefeature.domain.session.SessionProvider
import com.dino.message.crashfeature.data.remote.CrashLogApiService
import com.dino.message.crashfeature.data.service.CrashReporter
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single<DinoOrderApplication> { androidApplication() as DinoOrderApplication }
    single<SecureKeyValueStore> { AndroidEncryptedSecureKeyValueStore(context = androidContext()) }
    single { SPrefManager(secureStore = get()) }
    single<SessionProvider> { SPrefSessionProvider(sPrefManager = get()) }
    single<AppLogger> { AndroidAppLogger }
    single<NetworkExceptionClassifier> { JvmNetworkExceptionClassifier }
    single { AndroidUploadContentProvider(contentResolver = androidContext().contentResolver) }
    single {
        createAppHttpClient(
            engineFactory = OkHttp,
            baseUrl = BuildConfig.BASE_URL,
            isDebug = BuildConfig.DEBUG,
            appLogger = get(),
        )
    }
    single {
        HttpAPIUtil(
            sPrefManager = get(),
            context = androidContext(),
            networkExceptionClassifier = get(),
            appLogger = get(),
        )
    }
    single { createSettingsDataStore(context = androidContext()) }
    single { DataStoreUtil(dataStore = get()) }
    single { CrashLogApiService(httpClient = get(), sPrefManager = get()) }
    single<RealtimeSocketTransportFactory> { AndroidSocketIoTransportFactory(baseUrl = BuildConfig.BASE_URL) }
    single<SocketOutboundQueueStore> { RoomSocketOutboundQueueStore(socketOutboundQueueDao = get()) }
    single {
        CrashReporter(
            context = androidContext(),
            crashLogDao = get(),
            crashLogApiService = get(),
            sPrefManager = get(),
        )
    }
    single {
        AppSocketManager(
            sprefManager = get(),
            crashReporter = get(),
            socketOutboundQueueStore = get(),
            transportFactory = get(),
        )
    }
    single<RealtimeSocketGateway> { get<AppSocketManager>() }
    single {
        InAppNotificationManager(
            context = androidContext(),
            appSocketManager = get(),
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
