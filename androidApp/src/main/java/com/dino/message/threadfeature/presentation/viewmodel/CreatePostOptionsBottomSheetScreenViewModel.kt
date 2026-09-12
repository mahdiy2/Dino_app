package com.dino.message.threadfeature.presentation.viewmodel

import com.dino.message.corefeature.domain.session.AuthSessionStore
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.presentation.viewmodel.ScopedStateHolder
import com.dino.message.groupfeature.domain.usecase.SaveGroupMessageUseCase
import com.dino.message.corefeature.presentation.navigation.AppRoute

class CreatePostOptionsBottomSheetScreenViewModel constructor(
    uiEventBus: UiEventBus,
    private val saveGroupMessageUseCase: SaveGroupMessageUseCase,
    private val sPrefManager: AuthSessionStore,
) : ScopedStateHolder(),
    HaveUIEvent by HaveUIEventImpl(uiEventBus) {

    fun openCreatePostScreen(quotedPostId: Long = 0L) {
        navigateToDestination(
            AppRoute.CreatePost(
                quotedPostId = quotedPostId
            ),
            viewModelScope
        )
    }

    fun openPostMusicScreen(quotedPostId: Long = 0L) {
        navigateToDestination(
            AppRoute.PostMusic(
                quotedPostId = quotedPostId
            ),
            viewModelScope
        )

    }

}
