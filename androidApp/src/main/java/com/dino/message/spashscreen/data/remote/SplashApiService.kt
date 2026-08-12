package com.dino.message.spashscreen.data.remote

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.spashscreen.domain.model.VersionCheckDecision

interface SplashApiService {
    suspend fun checkAppVersion(version: String): Resource<VersionCheckDecision>
}
