package com.dino.message.threadfeature.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.groupfeature.domain.usecase.SaveGroupMessageUseCase
import com.ramcosta.composedestinations.generated.destinations.CreatePostScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PostMusicScreenDestination

class CreatePostOptionsBottomSheetScreenViewModel constructor(
    app: Context,
    private val saveGroupMessageUseCase: SaveGroupMessageUseCase,
    private val sPrefManager: SPrefManager,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    private val quotedPostId: Long = savedStateHandle.get<Long>("quotedPostId") ?: 0L
    val isQuoteMode: Boolean = quotedPostId > 0L

    fun openCreatePostScreen() {
        navigateToDestination(
            CreatePostScreenDestination(
                quotedPostId = quotedPostId
            ),
            viewModelScope
        )
    }

    fun openPostMusicScreen() {
        navigateToDestination(
            PostMusicScreenDestination(
                quotedPostId = quotedPostId
            ),
            viewModelScope
        )

    }

}
