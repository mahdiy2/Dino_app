package com.dino.message.corefeature.platform.resource

import com.dino.message.R
import com.dino.message.corefeature.domain.model.AppStringKey

fun AppStringKey.toAndroidStringResId(): Int = when (this) {
    AppStringKey.And -> R.string.and
    AppStringKey.ErrorBadRequest -> R.string.error_bad_request
    AppStringKey.ErrorUnauthorized -> R.string.error_unauthorized
    AppStringKey.ErrorForbidden -> R.string.error_forbidden
    AppStringKey.ErrorNotFound -> R.string.error_not_found
    AppStringKey.ErrorConflict -> R.string.error_conflict
    AppStringKey.ErrorTooManyLoginAttempts -> R.string.error_too_many_login_attempts
    AppStringKey.ErrorWithHttpStatusCode -> R.string.error_with_http_status_code
    AppStringKey.ErrorPayloadTooLarge -> R.string.error_payload_too_large
    AppStringKey.InternalServerError -> R.string.internal_server_error
    AppStringKey.ErrorConvertJsonResponse -> R.string.error_convert_json_response
    AppStringKey.UnknownErrorOccurred -> R.string.unknown_error_occurred
    AppStringKey.ErrorRequestTimedOut -> R.string.error_request_timed_out
    AppStringKey.NetworkError -> R.string.network_error
    AppStringKey.ErrorRetriesExceeded -> R.string.error_retries_exceeded
    AppStringKey.CannotReadFile -> R.string.error_cannot_read_file
    AppStringKey.UploadTimedOutWeakNetwork -> R.string.error_upload_timed_out_weak_network
    AppStringKey.AttachmentMimeTypeRequired -> R.string.error_attachment_mime_type_required
    AppStringKey.UnsupportedAttachmentTypeSecurity -> R.string.error_unsupported_attachment_type_security
    AppStringKey.UnsupportedAttachmentType -> R.string.error_unsupported_attachment_type
    AppStringKey.ImageAttachmentTooLarge -> R.string.error_image_attachment_too_large
    AppStringKey.VideoAttachmentTooLarge -> R.string.error_video_attachment_too_large
    AppStringKey.VoiceAttachmentTooLarge -> R.string.error_voice_attachment_too_large
    AppStringKey.FileAttachmentTooLarge -> R.string.error_file_attachment_too_large
    AppStringKey.MaxMediaFilesAllowed -> R.string.error_max_media_files_allowed
    AppStringKey.StoryVideoDurationLimit -> R.string.error_story_video_duration_limit
    AppStringKey.ReportPostSelectTypeError -> R.string.report_post_select_type_error
    AppStringKey.GroupCreateErrorEmptyName -> R.string.group_create_error_empty_name
    AppStringKey.GroupCreateErrorNameTooLong -> R.string.group_create_error_name_too_long
    AppStringKey.GroupFallbackName -> R.string.group_fallback_name
    AppStringKey.GroupAddMembersErrorSelectOne -> R.string.group_add_members_error_select_one
    AppStringKey.GroupAddMembersErrorAlreadyMember -> R.string.group_add_members_error_already_member
    AppStringKey.Success -> R.string.success
    AppStringKey.PlaylistUndoRemoveFailed -> R.string.playlist_undo_remove_failed
    AppStringKey.PlaylistRemovedMessage -> R.string.playlist_removed_message
    AppStringKey.Undo -> R.string.undo
    AppStringKey.PlaylistRemoveFailedRestored -> R.string.playlist_remove_failed_restored
    AppStringKey.LoginKeyRecoveryFailed -> R.string.login_key_recovery_failed
    AppStringKey.ErrorUsernameMinLength -> R.string.error_username_min_length
    AppStringKey.ErrorUsernameAllowedChars -> R.string.error_username_allowed_chars
    AppStringKey.ErrorPasswordMinLength -> R.string.error_password_min_length
    AppStringKey.ErrorPasswordRequireLetter -> R.string.error_password_require_letter
    AppStringKey.ErrorPasswordRequireNumber -> R.string.error_password_require_number
    AppStringKey.ErrorPasswordRequireSymbol -> R.string.error_password_require_symbol
    AppStringKey.ShareErrorSelectTarget -> R.string.share_error_select_target
    AppStringKey.ShareFailedTargetFormat -> R.string.share_failed_target_format
    AppStringKey.ShareGroupFallback -> R.string.share_group_fallback
    AppStringKey.ShareStatusQueued -> R.string.share_status_queued
    AppStringKey.ShareStatusPartial -> R.string.share_status_partial
    AppStringKey.ShareStatusFailed -> R.string.share_status_failed
    AppStringKey.ShareErrorFetchKey -> R.string.share_error_fetch_key
    AppStringKey.ShareErrorQueueFull -> R.string.share_error_queue_full
    AppStringKey.ShareErrorInvalidPublicKey -> R.string.share_error_invalid_public_key
    AppStringKey.ShareErrorEncryptionFailed -> R.string.share_error_encryption_failed
    AppStringKey.ShareErrorGroupNoKeys -> R.string.share_error_group_no_keys
    AppStringKey.ShareErrorInvalidGroupKeyData -> R.string.share_error_invalid_group_key_data
    AppStringKey.ShareErrorGroupEncryptionFailed -> R.string.share_error_group_encryption_failed
}
