package com.dino.message.di

import com.dino.message.activitynotificationfeature.domain.usecase.GetActivityNotificationsUseCase
import com.dino.message.activitynotificationfeature.domain.usecase.GetUnreadCountUseCase
import com.dino.message.activitynotificationfeature.domain.usecase.MarkNotificationsReadUseCase
import com.dino.message.activitynotificationfeature.presentation.viewmodel.ActivityNotificationViewModel
import com.dino.message.authfeature.domain.usecase.ConfirmRegistrationUseCase
import com.dino.message.authfeature.domain.usecase.GetKeyStatusUseCase
import com.dino.message.authfeature.domain.usecase.LoginUseCase
import com.dino.message.authfeature.domain.usecase.RegisterUseCase
import com.dino.message.authfeature.domain.usecase.RotatePublicKeyUseCase
import com.dino.message.authfeature.domain.usecase.SyncDeviceIdUseCase
import com.dino.message.authfeature.domain.usecase.UpdateDeviceIdUseCase
import com.dino.message.authfeature.presentation.viewmodel.LoginScreenViewModel
import com.dino.message.authfeature.presentation.viewmodel.PrivateKeyAlertScreenViewModel
import com.dino.message.authfeature.presentation.viewmodel.RegisterScreenViewModel
import com.dino.message.chatfeature.domain.usecase.ClearMessageFromDBUseCase
import com.dino.message.chatfeature.domain.usecase.GetMessageFromDBUseCase
import com.dino.message.chatfeature.domain.usecase.GetReceiverPublicKeyUseCase
import com.dino.message.chatfeature.domain.usecase.SaveMessageInDbUseCase
import com.dino.message.chatfeature.domain.usecase.UploadAttachmentUseCase
import com.dino.message.chatfeature.presentation.viewmodel.ChatViewModel
import com.dino.message.corefeature.data.util.DeviceIdProvider
import com.dino.message.corefeature.domain.usecase.ClearLocalSessionUseCase
import com.dino.message.corefeature.presentation.viewmodel.CustomMessageDialogViewModel
import com.dino.message.corefeature.presentation.viewmodel.MainActivityViewModel
import com.dino.message.corefeature.presentation.viewmodel.PrivateKeyResetDialogViewModel
import com.dino.message.groupfeature.domain.usecase.AddGroupMembersUseCase
import com.dino.message.groupfeature.domain.usecase.CreateGroupUseCase
import com.dino.message.groupfeature.domain.usecase.GetGroupDetailUseCase
import com.dino.message.groupfeature.domain.usecase.GetGroupMessagesFromDbUseCase
import com.dino.message.groupfeature.domain.usecase.GetMutualFollowersUseCase
import com.dino.message.groupfeature.domain.usecase.ListGroupsUseCase
import com.dino.message.groupfeature.domain.usecase.SaveGroupMessageUseCase
import com.dino.message.groupfeature.domain.usecase.UploadGroupAttachmentUseCase
import com.dino.message.groupfeature.presentation.viewmodel.AddGroupMembersViewModel
import com.dino.message.groupfeature.presentation.viewmodel.CreateGroupViewModel
import com.dino.message.groupfeature.presentation.viewmodel.GroupChatViewModel
import com.dino.message.groupfeature.presentation.viewmodel.GroupMembersViewModel
import com.dino.message.mainpage.domain.usecase.AddContactUseCase
import com.dino.message.mainpage.domain.usecase.GetContactsDeltaUseCase
import com.dino.message.mainpage.domain.usecase.GetContactsUseCase
import com.dino.message.mainpage.domain.usecase.GetDetailedContactsUseCase
import com.dino.message.mainpage.presentation.viewmodel.MainScreenViewModel
import com.dino.message.profilefeature.domain.usecase.DeleteMyAccountUseCase
import com.dino.message.profilefeature.domain.usecase.FollowUserUseCase
import com.dino.message.profilefeature.domain.usecase.GetAboutUsUseCase
import com.dino.message.profilefeature.domain.usecase.GetBlockedUsersUseCase
import com.dino.message.profilefeature.domain.usecase.GetFollowStatusUseCase
import com.dino.message.profilefeature.domain.usecase.GetFollowersUseCase
import com.dino.message.profilefeature.domain.usecase.GetFollowingUseCase
import com.dino.message.profilefeature.domain.usecase.GetMyProfileUseCase
import com.dino.message.profilefeature.domain.usecase.GetMyReferralSummaryUseCase
import com.dino.message.profilefeature.domain.usecase.GetProfilePostsUseCase
import com.dino.message.profilefeature.domain.usecase.GetProfileUseCase
import com.dino.message.profilefeature.domain.usecase.RemoveFollowerUseCase
import com.dino.message.profilefeature.domain.usecase.UnblockUserUseCase
import com.dino.message.profilefeature.domain.usecase.UnfollowUserUseCase
import com.dino.message.profilefeature.domain.usecase.UpdateMyProfileUseCase
import com.dino.message.profilefeature.presentation.viewmodel.AboutUsViewModel
import com.dino.message.profilefeature.presentation.viewmodel.BlockListViewModel
import com.dino.message.profilefeature.presentation.viewmodel.DeletePostBottomSheetViewModel
import com.dino.message.profilefeature.presentation.viewmodel.EditProfileViewModel
import com.dino.message.profilefeature.presentation.viewmodel.ProfileFollowListViewModel
import com.dino.message.profilefeature.presentation.viewmodel.ProfileScreenViewModel
import com.dino.message.profilefeature.presentation.viewmodel.ProfileSettingsViewModel
import com.dino.message.profilefeature.presentation.viewmodel.ReferralViewModel
import com.dino.message.reportfeature.domain.usecase.CreatePostReportUseCase
import com.dino.message.reportfeature.domain.usecase.GetReportTypesUseCase
import com.dino.message.reportfeature.presentation.viewmodel.ReportPostBottomSheetViewModel
import com.dino.message.searchfeature.domain.usecase.SearchAllUseCase
import com.dino.message.searchfeature.domain.usecase.SearchPostsUseCase
import com.dino.message.searchfeature.domain.usecase.SearchUsersUseCase
import com.dino.message.searchfeature.presentation.viewmodel.SearchViewModel
import com.dino.message.spashscreen.viewmodel.SpashViewModel
import com.dino.message.threadfeature.domain.usecase.AddTrackToPlaylistUseCase
import com.dino.message.threadfeature.domain.usecase.BlockUserUseCase
import com.dino.message.threadfeature.domain.usecase.CreateCommentUseCase
import com.dino.message.threadfeature.domain.usecase.CreatePostUseCase
import com.dino.message.threadfeature.domain.usecase.DeleteCommentUseCase
import com.dino.message.threadfeature.domain.usecase.DeletePostUseCase
import com.dino.message.threadfeature.domain.usecase.DeleteStoryUseCase
import com.dino.message.threadfeature.domain.usecase.EditPostUseCase
import com.dino.message.threadfeature.domain.usecase.GetCommentsUseCase
import com.dino.message.threadfeature.domain.usecase.GetPlaylistTracksUseCase
import com.dino.message.threadfeature.domain.usecase.GetPostUseCase
import com.dino.message.threadfeature.domain.usecase.GetPostsUseCase
import com.dino.message.threadfeature.domain.usecase.GetStoryBundleUseCase
import com.dino.message.threadfeature.domain.usecase.GetStoryFeedUseCase
import com.dino.message.threadfeature.domain.usecase.GetStoryMentionCandidatesUseCase
import com.dino.message.threadfeature.domain.usecase.GetStoryViewersUseCase
import com.dino.message.threadfeature.domain.usecase.IsTrackInPlaylistUseCase
import com.dino.message.threadfeature.domain.usecase.RecordStoryViewUseCase
import com.dino.message.threadfeature.domain.usecase.RemoveTrackFromPlaylistUseCase
import com.dino.message.threadfeature.domain.usecase.ReplyToStoryUseCase
import com.dino.message.threadfeature.domain.usecase.SetStoryLikeUseCase
import com.dino.message.threadfeature.domain.usecase.UploadStoryUseCase
import com.dino.message.threadfeature.domain.usecase.VoteUseCase
import com.dino.message.threadfeature.presentation.viewmodel.CreatePostOptionsBottomSheetScreenViewModel
import com.dino.message.threadfeature.presentation.viewmodel.CreatePostViewModel
import com.dino.message.threadfeature.presentation.viewmodel.PlaylistBottomSheetViewModel
import com.dino.message.threadfeature.presentation.viewmodel.PostMusicViewModel
import com.dino.message.threadfeature.presentation.viewmodel.SharePostBottomSheetViewModel
import com.dino.message.threadfeature.presentation.viewmodel.SharedPostDetailsViewModel
import com.dino.message.threadfeature.presentation.viewmodel.ThreadCommentsViewModel
import com.dino.message.threadfeature.presentation.viewmodel.ThreadViewModel
import com.dino.message.threadfeature.presentation.viewmodel.TrackPlaylistAddersDialogViewModel
import com.dino.message.updatefeature.presentation.viewmodel.AppUpdateViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    factory { DeviceIdProvider(context = androidContext()) }
    factory { ClearLocalSessionUseCase(appContext = androidContext(), sPrefManager = get(), messageDatabase = get()) }

    factoryOf(::GetActivityNotificationsUseCase)
    factoryOf(::GetUnreadCountUseCase)
    factoryOf(::MarkNotificationsReadUseCase)
    factoryOf(::ConfirmRegistrationUseCase)
    factoryOf(::GetKeyStatusUseCase)
    factoryOf(::LoginUseCase)
    factoryOf(::RegisterUseCase)
    factoryOf(::RotatePublicKeyUseCase)
    factoryOf(::SyncDeviceIdUseCase)
    factoryOf(::UpdateDeviceIdUseCase)
    factoryOf(::ClearMessageFromDBUseCase)
    factoryOf(::GetMessageFromDBUseCase)
    factoryOf(::GetReceiverPublicKeyUseCase)
    factoryOf(::SaveMessageInDbUseCase)
    factoryOf(::UploadAttachmentUseCase)
    factoryOf(::AddGroupMembersUseCase)
    factoryOf(::CreateGroupUseCase)
    factoryOf(::GetGroupDetailUseCase)
    factoryOf(::GetGroupMessagesFromDbUseCase)
    factoryOf(::GetMutualFollowersUseCase)
    factoryOf(::ListGroupsUseCase)
    factoryOf(::SaveGroupMessageUseCase)
    factoryOf(::UploadGroupAttachmentUseCase)
    factoryOf(::AddContactUseCase)
    factoryOf(::GetContactsDeltaUseCase)
    factoryOf(::GetContactsUseCase)
    factoryOf(::GetDetailedContactsUseCase)
    factoryOf(::DeleteMyAccountUseCase)
    factoryOf(::FollowUserUseCase)
    factoryOf(::GetAboutUsUseCase)
    factoryOf(::GetBlockedUsersUseCase)
    factoryOf(::GetFollowStatusUseCase)
    factoryOf(::GetFollowersUseCase)
    factoryOf(::GetFollowingUseCase)
    factoryOf(::GetMyProfileUseCase)
    factoryOf(::GetMyReferralSummaryUseCase)
    factoryOf(::GetProfilePostsUseCase)
    factoryOf(::GetProfileUseCase)
    factoryOf(::RemoveFollowerUseCase)
    factoryOf(::UnblockUserUseCase)
    factoryOf(::UnfollowUserUseCase)
    factoryOf(::UpdateMyProfileUseCase)
    factoryOf(::CreatePostReportUseCase)
    factoryOf(::GetReportTypesUseCase)
    factoryOf(::SearchAllUseCase)
    factoryOf(::SearchPostsUseCase)
    factoryOf(::SearchUsersUseCase)
    factoryOf(::AddTrackToPlaylistUseCase)
    factoryOf(::BlockUserUseCase)
    factoryOf(::CreateCommentUseCase)
    factoryOf(::CreatePostUseCase)
    factoryOf(::DeleteCommentUseCase)
    factoryOf(::DeletePostUseCase)
    factoryOf(::DeleteStoryUseCase)
    factoryOf(::EditPostUseCase)
    factoryOf(::GetCommentsUseCase)
    factoryOf(::GetPlaylistTracksUseCase)
    factoryOf(::GetPostUseCase)
    factoryOf(::GetPostsUseCase)
    factoryOf(::GetStoryBundleUseCase)
    factoryOf(::GetStoryFeedUseCase)
    factoryOf(::GetStoryMentionCandidatesUseCase)
    factoryOf(::GetStoryViewersUseCase)
    factoryOf(::IsTrackInPlaylistUseCase)
    factoryOf(::RecordStoryViewUseCase)
    factoryOf(::RemoveTrackFromPlaylistUseCase)
    factoryOf(::ReplyToStoryUseCase)
    factoryOf(::SetStoryLikeUseCase)
    factoryOf(::UploadStoryUseCase)
    factoryOf(::VoteUseCase)

    viewModelOf(::ActivityNotificationViewModel)
    viewModelOf(::LoginScreenViewModel)
    viewModelOf(::PrivateKeyAlertScreenViewModel)
    viewModelOf(::RegisterScreenViewModel)
    viewModelOf(::ChatViewModel)
    viewModelOf(::CustomMessageDialogViewModel)
    viewModelOf(::MainActivityViewModel)
    viewModelOf(::PrivateKeyResetDialogViewModel)
    viewModelOf(::AddGroupMembersViewModel)
    viewModelOf(::CreateGroupViewModel)
    viewModelOf(::GroupChatViewModel)
    viewModelOf(::GroupMembersViewModel)
    viewModelOf(::MainScreenViewModel)
    viewModelOf(::AboutUsViewModel)
    viewModelOf(::BlockListViewModel)
    viewModelOf(::DeletePostBottomSheetViewModel)
    viewModelOf(::EditProfileViewModel)
    viewModelOf(::ProfileFollowListViewModel)
    viewModelOf(::ProfileScreenViewModel)
    viewModelOf(::ProfileSettingsViewModel)
    viewModelOf(::ReferralViewModel)
    viewModelOf(::ReportPostBottomSheetViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::SpashViewModel)
    viewModelOf(::CreatePostOptionsBottomSheetScreenViewModel)
    viewModelOf(::CreatePostViewModel)
    viewModelOf(::PlaylistBottomSheetViewModel)
    viewModelOf(::PostMusicViewModel)
    viewModelOf(::SharePostBottomSheetViewModel)
    viewModelOf(::SharedPostDetailsViewModel)
    viewModelOf(::ThreadCommentsViewModel)
    viewModelOf(::ThreadViewModel)
    viewModelOf(::TrackPlaylistAddersDialogViewModel)
    viewModelOf(::AppUpdateViewModel)
}
