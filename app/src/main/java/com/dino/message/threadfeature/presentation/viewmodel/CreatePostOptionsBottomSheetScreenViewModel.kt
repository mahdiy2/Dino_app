package com.dino.message.threadfeature.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.groupfeature.domain.usecase.SaveGroupMessageUseCase
import com.dino.message.corefeature.presentation.navigation.AppRoute

class CreatePostOptionsBottomSheetScreenViewModel constructor(
    app: Context,
    private val saveGroupMessageUseCase: SaveGroupMessageUseCase,
    private val sPrefManager: SPrefManager,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

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
