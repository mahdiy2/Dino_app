package com.dino.message.corefeature.domain.model

import com.dino.message.corefeature.domain.result.Resource
import kotlinx.serialization.Serializable

@Serializable
class CustomMessageDialogData private constructor(
    val messages: List<CustomMessageDialogContentItem>,
    val type: String
) {
    class CustomMessageDialogBuilder {
        private var messages: MutableList<CustomMessageDialogContentItem> = mutableListOf()
        private var type: String = CustomMessageDialogType.Empty.type

        fun addMessage(
            title: StringResourceContent,
            description: StringResourceContent
        ): CustomMessageDialogBuilder = addMessage(
            title = title,
            descriptions = listOf(description)
        )

        fun addMessage(
            title: StringResourceContent,
            descriptions: List<StringResourceContent>
        ): CustomMessageDialogBuilder {
            messages.add(
                CustomMessageDialogContentItem(
                    title = title,
                    descriptions = descriptions.toMutableList()
                )
            )
            return this
        }

        fun <T> addMessage(
            title: StringResourceContent,
            resourceError: com.dino.message.corefeature.domain.result.Resource.Error<T>
        ): CustomMessageDialogBuilder = addMessage(
            title = title,
            descriptions = listOf(resourceError.message)
        )

        fun setType(value: CustomMessageDialogType): CustomMessageDialogBuilder {
            type = value.type
            return this
        }

        fun isMessageListNotEmpty(): Boolean = messages.isNotEmpty()

        fun build() = CustomMessageDialogData(
            messages = messages.toSet().toMutableList(),
            type = type
        )
    }
}
