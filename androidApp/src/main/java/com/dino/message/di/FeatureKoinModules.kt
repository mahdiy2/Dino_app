package com.dino.message.di

import com.dino.message.activitynotificationfeature.data.remote.ActivityNotificationApiService
import com.dino.message.activitynotificationfeature.data.remote.ActivityNotificationApiServiceImpl
import com.dino.message.activitynotificationfeature.data.repository.ActivityNotificationRepositoryImpl
import com.dino.message.activitynotificationfeature.domain.repository.ActivityNotificationRepository
import com.dino.message.authfeature.data.remote.AuthAPIService
import com.dino.message.authfeature.data.remote.AuthAPIServiceImpl
import com.dino.message.authfeature.data.repository.AuthRepositoryImpl
import com.dino.message.authfeature.domain.repository.AuthRepository
import com.dino.message.chatfeature.data.remote.ChatApiService
import com.dino.message.chatfeature.data.remote.ChatApiServiceImpl
import com.dino.message.chatfeature.data.repository.ChatRepositoryImpl
import com.dino.message.chatfeature.domain.repository.ChatRepository
import com.dino.message.groupfeature.data.remote.GroupApiService
import com.dino.message.groupfeature.data.remote.GroupApiServiceImpl
import com.dino.message.groupfeature.data.repository.GroupRepositoryImpl
import com.dino.message.groupfeature.domain.repository.GroupRepository
import com.dino.message.mainpage.data.remote.MainApiService
import com.dino.message.mainpage.data.remote.MainApiServiceImpl
import com.dino.message.mainpage.data.repository.MainRepositoryImpl
import com.dino.message.mainpage.domain.repository.MainRepository
import com.dino.message.profilefeature.data.remote.ProfileApiService
import com.dino.message.profilefeature.data.remote.ProfileApiServiceImpl
import com.dino.message.profilefeature.data.repository.ProfileRepositoryImpl
import com.dino.message.profilefeature.domain.repository.ProfileRepository
import com.dino.message.reportfeature.data.remote.ReportApiService
import com.dino.message.reportfeature.data.remote.ReportApiServiceImpl
import com.dino.message.reportfeature.data.repository.ReportRepositoryImpl
import com.dino.message.reportfeature.domain.repository.ReportRepository
import com.dino.message.searchfeature.data.remote.SearchApiService
import com.dino.message.searchfeature.data.remote.SearchApiServiceImpl
import com.dino.message.searchfeature.data.repository.SearchRepositoryImpl
import com.dino.message.searchfeature.domain.repository.SearchRepository
import com.dino.message.spashscreen.data.remote.SplashApiServiceImpl
import com.dino.message.spashscreen.domain.repository.SplashApiService
import com.dino.message.threadfeature.data.remote.ThreadApiService
import com.dino.message.threadfeature.data.remote.ThreadApiServiceImpl
import com.dino.message.threadfeature.data.repository.ThreadRepositoryImpl
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val authModule = module {
    single<AuthAPIService> {
        AuthAPIServiceImpl(
            httpClient = get(),
            sPrefManager = get(),
            httpAPIUtil = get(),
        )
    }
    single<AuthRepository> { AuthRepositoryImpl(authAPIService = get()) }
}

val chatModule = module {
    single<ChatApiService> {
        ChatApiServiceImpl(
            httpClient = get(),
            sPrefManager = get(),
            httpAPIUtil = get(),
            uploadContentProvider = get(),
            stringResolver = get(),
            appClock = get(),
            appLogger = get(),
        )
    }
    single<ChatRepository> {
        ChatRepositoryImpl(
            chatApiService = get(),
            dao = get(),
        )
    }
}

val threadModule = module {
    single<ThreadApiService> {
        ThreadApiServiceImpl(
            httpClient = get(),
            sPrefManager = get(),
            httpAPIUtil = get(),
            crashReporter = get(),
            uploadContentProvider = get(),
            stringResolver = get(),
            mediaMetadataExtractor = get(),
        )
    }
    single<ThreadRepository> { ThreadRepositoryImpl(threadApiService = get()) }
}

val profileModule = module {
    single<ProfileApiService> {
        ProfileApiServiceImpl(
            httpClient = get(),
            sPrefManager = get(),
            httpAPIUtil = get(),
            uploadContentProvider = get(),
            appContext = androidContext(),
        )
    }
    single<ProfileRepository> { ProfileRepositoryImpl(profileApiService = get()) }
}

val groupModule = module {
    single<GroupApiService> {
        GroupApiServiceImpl(
            httpClient = get(),
            sPrefManager = get(),
            httpAPIUtil = get(),
            uploadContentProvider = get(),
            stringResolver = get(),
            appClock = get(),
            appLogger = get(),
        )
    }
    single<GroupRepository> {
        GroupRepositoryImpl(
            apiService = get(),
            groupMessageDao = get(),
        )
    }
}

val mainModule = module {
    single<MainApiService> {
        MainApiServiceImpl(
            httpClient = get(),
            sPrefManager = get(),
            httpAPIUtil = get(),
        )
    }
    single<MainRepository> { MainRepositoryImpl(mainApiService = get()) }
}

val searchModule = module {
    single<SearchApiService> {
        SearchApiServiceImpl(
            httpClient = get(),
            sPrefManager = get(),
            httpAPIUtil = get(),
        )
    }
    single<SearchRepository> { SearchRepositoryImpl(searchApiService = get()) }
}

val reportModule = module {
    single<ReportApiService> {
        ReportApiServiceImpl(
            httpClient = get(),
            sPrefManager = get(),
            httpAPIUtil = get(),
        )
    }
    single<ReportRepository> { ReportRepositoryImpl(reportApiService = get()) }
}

val activityNotificationModule = module {
    single<ActivityNotificationApiService> {
        ActivityNotificationApiServiceImpl(
            httpClient = get(),
            sPrefManager = get(),
            httpAPIUtil = get(),
        )
    }
    single<ActivityNotificationRepository> {
        ActivityNotificationRepositoryImpl(apiService = get())
    }
}

val splashModule = module {
    single<SplashApiService> {
        SplashApiServiceImpl(
            httpClient = get(),
            httpAPIUtil = get(),
        )
    }
}

val updateModule = module {
}
