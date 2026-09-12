package com.dino.message.corefeature.presentation.util

import com.dino.message.corefeature.domain.model.CustomMessageDialogData
import com.dino.message.corefeature.domain.model.ImageDialogData
import com.dino.message.corefeature.presentation.navigation.AppRoute
import kotlinx.serialization.Serializable

@Serializable
sealed class UIEvent {
    @Serializable
    data class NavigateWithPopBackStack(
        val route: AppRoute,
        val level: PopBackStackLevel
    ) : UIEvent()

    @Serializable
    data class Navigate(
        val route: AppRoute
    ) : UIEvent()

    @Serializable
    data object NavigateUp : UIEvent()

    @Serializable
    data class ShowCustomMessageDialog(
        val data: CustomMessageDialogData
    ) : UIEvent()

    @Serializable
    data class ShowImageDialog(
        val data: ImageDialogData
    ) : UIEvent()
}
