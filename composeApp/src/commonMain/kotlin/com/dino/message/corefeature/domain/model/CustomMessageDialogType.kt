package com.dino.message.corefeature.domain.model

sealed class CustomMessageDialogType(val type: String) {
    data object Empty : CustomMessageDialogType("Empty")
    data object Error : CustomMessageDialogType("Error")
    data object Success : CustomMessageDialogType("Success")
}
