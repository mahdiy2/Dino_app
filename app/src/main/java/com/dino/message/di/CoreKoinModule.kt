package com.dino.message.di

import android.content.Context
import androidx.room.Room
import com.dino.message.BuildConfig
import com.dino.message.DinoOrderApplication
import com.dino.message.chatfeature.data.notification.InAppNotificationManager
import com.dino.message.corefeature.data.db.MessageDatabase
import com.dino.message.corefeature.data.remote.util.HttpAPIUtil
import com.dino.message.corefeature.data.session.SPrefSessionProvider
import com.dino.message.corefeature.data.socket.AppSocketManager
import com.dino.message.corefeature.data.spref.DataStoreUtil
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.session.SessionProvider
import com.dino.message.crashfeature.data.remote.CrashLogApiService
import com.dino.message.crashfeature.data.service.CrashReporter
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single<DinoOrderApplication> { androidApplication() as DinoOrderApplication }
    single<Context> { androidContext() }
    single { Gson() }
    single { SPrefManager(gson = get(), context = androidContext()) }
    single<SessionProvider> { SPrefSessionProvider(sPrefManager = get()) }
    single { provideHttpClient() }
    single { HttpAPIUtil(sPrefManager = get(), context = androidContext()) }
    single { DataStoreUtil(context = androidContext()) }
    single { CrashLogApiService(httpClient = get(), sPrefManager = get()) }
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
            socketOutboundQueueDao = get(),
        )
    }
    single {
        InAppNotificationManager(
            context = androidContext(),
            appSocketManager = get(),
        )
    }
}

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            MessageDatabase::class.java,
            "chat_db"
        )
            .addMigrations(
                MessageDatabase.MIGRATION_1_2,
                MessageDatabase.MIGRATION_2_3,
                MessageDatabase.MIGRATION_3_4,
                MessageDatabase.MIGRATION_4_5,
                MessageDatabase.MIGRATION_5_6,
                MessageDatabase.MIGRATION_6_7,
                MessageDatabase.MIGRATION_7_8,
                MessageDatabase.MIGRATION_8_9,
                MessageDatabase.MIGRATION_9_10,
                MessageDatabase.MIGRATION_10_11,
                MessageDatabase.MIGRATION_11_12,
                MessageDatabase.MIGRATION_12_13,
            )
            .build()
    }
    single { get<MessageDatabase>().messageDao() }
    single { get<MessageDatabase>().groupMessageDao() }
    single { get<MessageDatabase>().crashLogDao() }
    single { get<MessageDatabase>().socketOutboundQueueDao() }
}

@OptIn(ExperimentalSerializationApi::class)
private fun provideHttpClient() =
    HttpClient(OkHttp) {
        defaultRequest {
            url(BuildConfig.BASE_URL)
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.HEADERS
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 20_000
            socketTimeoutMillis = 20_000
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            )
        }
    }
