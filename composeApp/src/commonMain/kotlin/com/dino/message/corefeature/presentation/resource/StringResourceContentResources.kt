package com.dino.message.corefeature.presentation.resource

import androidx.compose.runtime.Composable
import com.dino.message.composeapp.generated.resources.Res
import com.dino.message.composeapp.generated.resources.and
import com.dino.message.composeapp.generated.resources.error_attachment_mime_type_required
import com.dino.message.composeapp.generated.resources.error_bad_request
import com.dino.message.composeapp.generated.resources.error_cannot_read_file
import com.dino.message.composeapp.generated.resources.error_conflict
import com.dino.message.composeapp.generated.resources.error_convert_json_response
import com.dino.message.composeapp.generated.resources.error_file_attachment_too_large
import com.dino.message.composeapp.generated.resources.error_forbidden
import com.dino.message.composeapp.generated.resources.error_image_attachment_too_large
import com.dino.message.composeapp.generated.resources.error_max_media_files_allowed
import com.dino.message.composeapp.generated.resources.error_not_found
import com.dino.message.composeapp.generated.resources.error_password_min_length
import com.dino.message.composeapp.generated.resources.error_password_require_letter
import com.dino.message.composeapp.generated.resources.error_password_require_number
import com.dino.message.composeapp.generated.resources.error_password_require_symbol
import com.dino.message.composeapp.generated.resources.error_payload_too_large
import com.dino.message.composeapp.generated.resources.error_request_timed_out
import com.dino.message.composeapp.generated.resources.error_retries_exceeded
import com.dino.message.composeapp.generated.resources.error_story_video_duration_limit
import com.dino.message.composeapp.generated.resources.error_too_many_login_attempts
import com.dino.message.composeapp.generated.resources.error_unauthorized
import com.dino.message.composeapp.generated.resources.error_upload_timed_out_weak_network
import com.dino.message.composeapp.generated.resources.error_unsupported_attachment_type
import com.dino.message.composeapp.generated.resources.error_unsupported_attachment_type_security
import com.dino.message.composeapp.generated.resources.error_username_allowed_chars
import com.dino.message.composeapp.generated.resources.error_username_min_length
import com.dino.message.composeapp.generated.resources.error_video_attachment_too_large
import com.dino.message.composeapp.generated.resources.error_voice_attachment_too_large
import com.dino.message.composeapp.generated.resources.error_with_http_status_code
import com.dino.message.composeapp.generated.resources.group_add_members_error_already_member
import com.dino.message.composeapp.generated.resources.group_add_members_error_select_one
import com.dino.message.composeapp.generated.resources.group_create_error_empty_name
import com.dino.message.composeapp.generated.resources.group_create_error_name_too_long
import com.dino.message.composeapp.generated.resources.group_fallback_name
import com.dino.message.composeapp.generated.resources.internal_server_error
import com.dino.message.composeapp.generated.resources.login_key_recovery_failed
import com.dino.message.composeapp.generated.resources.network_error
import com.dino.message.composeapp.generated.resources.playlist_remove_failed_restored
import com.dino.message.composeapp.generated.resources.playlist_removed_message
import com.dino.message.composeapp.generated.resources.playlist_undo_remove_failed
import com.dino.message.composeapp.generated.resources.report_post_select_type_error
import com.dino.message.composeapp.generated.resources.share_error_encryption_failed
import com.dino.message.composeapp.generated.resources.share_error_fetch_key
import com.dino.message.composeapp.generated.resources.share_error_group_no_keys
import com.dino.message.composeapp.generated.resources.share_error_group_encryption_failed
import com.dino.message.composeapp.generated.resources.share_error_invalid_group_key_data
import com.dino.message.composeapp.generated.resources.share_error_invalid_public_key
import com.dino.message.composeapp.generated.resources.share_error_queue_full
import com.dino.message.composeapp.generated.resources.share_error_select_target
import com.dino.message.composeapp.generated.resources.share_failed_target_format
import com.dino.message.composeapp.generated.resources.share_group_fallback
import com.dino.message.composeapp.generated.resources.share_status_failed
import com.dino.message.composeapp.generated.resources.share_status_partial
import com.dino.message.composeapp.generated.resources.share_status_queued
import com.dino.message.composeapp.generated.resources.success
import com.dino.message.composeapp.generated.resources.unknown_error_occurred
import com.dino.message.composeapp.generated.resources.undo
import com.dino.message.corefeature.domain.model.AppStringKey
import com.dino.message.corefeature.domain.model.StringResourceContent
import com.dino.message.corefeature.domain.model.toFormatArgs
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun StringResourceContent.asComposeString(): String {
    return when (this) {
        is StringResourceContent.Str -> data
        is StringResourceContent.Key -> stringResource(key.toComposeStringResource(), *args.toFormatArgs())
    }
}

fun AppStringKey.toComposeStringResource(): StringResource = when (this) {
    AppStringKey.And -> Res.string.and
    AppStringKey.ErrorBadRequest -> Res.string.error_bad_request
    AppStringKey.ErrorUnauthorized -> Res.string.error_unauthorized
    AppStringKey.ErrorForbidden -> Res.string.error_forbidden
    AppStringKey.ErrorNotFound -> Res.string.error_not_found
    AppStringKey.ErrorConflict -> Res.string.error_conflict
    AppStringKey.ErrorTooManyLoginAttempts -> Res.string.error_too_many_login_attempts
    AppStringKey.ErrorWithHttpStatusCode -> Res.string.error_with_http_status_code
    AppStringKey.ErrorPayloadTooLarge -> Res.string.error_payload_too_large
    AppStringKey.InternalServerError -> Res.string.internal_server_error
    AppStringKey.ErrorConvertJsonResponse -> Res.string.error_convert_json_response
    AppStringKey.UnknownErrorOccurred -> Res.string.unknown_error_occurred
    AppStringKey.ErrorRequestTimedOut -> Res.string.error_request_timed_out
    AppStringKey.NetworkError -> Res.string.network_error
    AppStringKey.ErrorRetriesExceeded -> Res.string.error_retries_exceeded
    AppStringKey.CannotReadFile -> Res.string.error_cannot_read_file
    AppStringKey.UploadTimedOutWeakNetwork -> Res.string.error_upload_timed_out_weak_network
    AppStringKey.AttachmentMimeTypeRequired -> Res.string.error_attachment_mime_type_required
    AppStringKey.UnsupportedAttachmentTypeSecurity -> Res.string.error_unsupported_attachment_type_security
    AppStringKey.UnsupportedAttachmentType -> Res.string.error_unsupported_attachment_type
    AppStringKey.ImageAttachmentTooLarge -> Res.string.error_image_attachment_too_large
    AppStringKey.VideoAttachmentTooLarge -> Res.string.error_video_attachment_too_large
    AppStringKey.VoiceAttachmentTooLarge -> Res.string.error_voice_attachment_too_large
    AppStringKey.FileAttachmentTooLarge -> Res.string.error_file_attachment_too_large
    AppStringKey.MaxMediaFilesAllowed -> Res.string.error_max_media_files_allowed
    AppStringKey.StoryVideoDurationLimit -> Res.string.error_story_video_duration_limit
    AppStringKey.ReportPostSelectTypeError -> Res.string.report_post_select_type_error
    AppStringKey.GroupCreateErrorEmptyName -> Res.string.group_create_error_empty_name
    AppStringKey.GroupCreateErrorNameTooLong -> Res.string.group_create_error_name_too_long
    AppStringKey.GroupFallbackName -> Res.string.group_fallback_name
    AppStringKey.GroupAddMembersErrorSelectOne -> Res.string.group_add_members_error_select_one
    AppStringKey.GroupAddMembersErrorAlreadyMember -> Res.string.group_add_members_error_already_member
    AppStringKey.Success -> Res.string.success
    AppStringKey.PlaylistUndoRemoveFailed -> Res.string.playlist_undo_remove_failed
    AppStringKey.PlaylistRemovedMessage -> Res.string.playlist_removed_message
    AppStringKey.Undo -> Res.string.undo
    AppStringKey.PlaylistRemoveFailedRestored -> Res.string.playlist_remove_failed_restored
    AppStringKey.LoginKeyRecoveryFailed -> Res.string.login_key_recovery_failed
    AppStringKey.ErrorUsernameMinLength -> Res.string.error_username_min_length
    AppStringKey.ErrorUsernameAllowedChars -> Res.string.error_username_allowed_chars
    AppStringKey.ErrorPasswordMinLength -> Res.string.error_password_min_length
    AppStringKey.ErrorPasswordRequireLetter -> Res.string.error_password_require_letter
    AppStringKey.ErrorPasswordRequireNumber -> Res.string.error_password_require_number
    AppStringKey.ErrorPasswordRequireSymbol -> Res.string.error_password_require_symbol
    AppStringKey.ShareErrorSelectTarget -> Res.string.share_error_select_target
    AppStringKey.ShareFailedTargetFormat -> Res.string.share_failed_target_format
    AppStringKey.ShareGroupFallback -> Res.string.share_group_fallback
    AppStringKey.ShareStatusQueued -> Res.string.share_status_queued
    AppStringKey.ShareStatusPartial -> Res.string.share_status_partial
    AppStringKey.ShareStatusFailed -> Res.string.share_status_failed
    AppStringKey.ShareErrorFetchKey -> Res.string.share_error_fetch_key
    AppStringKey.ShareErrorQueueFull -> Res.string.share_error_queue_full
    AppStringKey.ShareErrorInvalidPublicKey -> Res.string.share_error_invalid_public_key
    AppStringKey.ShareErrorEncryptionFailed -> Res.string.share_error_encryption_failed
    AppStringKey.ShareErrorGroupNoKeys -> Res.string.share_error_group_no_keys
    AppStringKey.ShareErrorInvalidGroupKeyData -> Res.string.share_error_invalid_group_key_data
    AppStringKey.ShareErrorGroupEncryptionFailed -> Res.string.share_error_group_encryption_failed
}
