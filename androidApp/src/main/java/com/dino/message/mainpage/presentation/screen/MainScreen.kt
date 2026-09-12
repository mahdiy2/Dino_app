package com.dino.message.mainpage.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import com.dino.message.R
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.backIconModifier
import com.dino.message.corefeature.presentation.platform.LocalPlatformUiEffect
import com.dino.message.errorfeature.presentation.screen.ErrorScreen
import com.dino.message.mainpage.presentation.component.GroupListItem
import com.dino.message.mainpage.presentation.component.MessageListItems
import com.dino.message.mainpage.presentation.viewmodel.MainScreenViewModel
import com.dino.message.nointernetfeature.presentation.screen.NoInternetScreen
import com.dino.message.threadfeature.presentation.component.NowPlayingTopBarRow
import com.dino.message.threadfeature.presentation.music.MusicPlaybackManager
import kotlinx.coroutines.delay
import com.dino.message.corefeature.presentation.icons.AppIcons


@OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun MainScreen() {
    val viewModel: MainScreenViewModel = rememberStateHolder()
    val platformUiEffect = LocalPlatformUiEffect.current
    var isNoInternet by remember { mutableStateOf(!viewModel.isConnected()) }

    val listState = remember { LazyListState() }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isSheetVisible by viewModel.isBottomSheetVisible.collectAsState()

    var toolbarExpanded by rememberSaveable { mutableStateOf(false) }
    var showMessageSecurityDialog by rememberSaveable { mutableStateOf(false) }

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val playbackState by MusicPlaybackManager.playbackState.collectAsState()
    val hasNowPlaying = !playbackState.currentTrackUrl.isNullOrBlank()
    var nowPlayingHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val nowPlayingHeightDp = with(density) { nowPlayingHeightPx.toDp() }
    val reservedNowPlayingHeight =
        if (!hasNowPlaying) 0.dp else nowPlayingHeightDp.takeIf { it > 0.dp } ?: 64.dp


    LaunchedEffect(Unit) {
        delay(1000)
        toolbarExpanded = true
    }
    LaunchedEffect(hasNowPlaying) {
        if (!hasNowPlaying) nowPlayingHeightPx = 0
    }

    LaunchedEffect(Unit) {
        while (true) {
            isNoInternet = !viewModel.isConnected()
            delay(2000)
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 3 && hasMore && !isLoadingMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadNextPage()
    }

    isNoInternet = !viewModel.isConnected()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(R.string.messages))
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateUp() }) {
                        Icon(
                            imageVector = AppIcons.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back),
                            modifier = backIconModifier()
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMessageSecurityDialog = true }) {
                        Icon(
                            imageVector = AppIcons.Info,
                            contentDescription = stringResource(R.string.message_security_info)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateToCreateGroupScreen() },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    imageVector = AppIcons.Group,
                    contentDescription = stringResource(R.string.group_create_title)
                )
            }
        }
    ) { innerPadding ->
        if (showMessageSecurityDialog) {
            AlertDialog(
                onDismissRequest = { showMessageSecurityDialog = false },
                title = { Text(stringResource(R.string.message_privacy_title)) },
                text = {
                    Text(
                        stringResource(R.string.message_privacy_body)
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showMessageSecurityDialog = false }) {
                        Text(stringResource(R.string.got_it))
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    viewModel.refresh()
                },
                modifier = Modifier
                    .fillMaxSize()

            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                )
                {
                    Column(
                        Modifier
                            .fillMaxSize()
                    ) {
                        when {
                            isNoInternet -> NoInternetScreen {
                                if (viewModel.isConnected()) {
                                    isNoInternet = false
                                }
                            }

                            viewModel.isError.value -> ErrorScreen {
                            }

                        }

                        if (viewModel.groupList.isEmpty() && viewModel.contactList.isEmpty() && !isRefreshing) {
                                Column(
                                    Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.start_follow),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = DPMedium, end = DPMedium, start = DPMedium)
                                    )

                                    Text(
                                        text = stringResource(R.string.main_no_contacts_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        text = stringResource(R.string.main_no_contacts_subtitle),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(Modifier.size(DPExtraLarge))

                                }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (reservedNowPlayingHeight > 0.dp) {
                                    item {
                                        Spacer(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(reservedNowPlayingHeight)
                                        )
                                    }
                                }


                                if (viewModel.groupList.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = stringResource(R.string.groups),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(
                                                start = DPMedium,
                                                top = DPSmall,
                                                bottom = DPSmall
                                            )
                                        )
                                    }
                                    items(viewModel.groupList) { group ->
                                        if (group.isPublicRoom) return@items
                                        GroupListItem(
                                            groupName = group.name,
                                            memberCount = group.memberCount,
                                            newMessage = group.hasUnread,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            viewModel.navigateToGroupChatScreen(group)
                                        }
                                    }
                                    item {
                                        Text(
                                            text = stringResource(R.string.contacts),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(
                                                start = DPMedium,
                                                top = DPMedium,
                                                bottom = DPSmall
                                            )
                                        )
                                    }
                                }

                                items(
                                    viewModel.contactList
                                ) {
                                    if (it != null)
                                        MessageListItems(
                                            displayName = it.displayName,
                                            badge = it.badge,
                                            time = it.time,
                                            newMessage = it.newMessage,
                                            profileImageUrl = it.profileImageUrl,
                                            profileImageShape = it.profileImageShape,
                                            profileVideoUrl = it.profileVideoUrl,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                        ) {
                                            viewModel.navigateToChatScreen(it.username)
                                        }
                                }

                                if (isLoadingMore) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                            }
                        }


                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .onGloballyPositioned { nowPlayingHeightPx = it.size.height }
                    ) {
                        NowPlayingTopBarRow(onOpenPlaylist = viewModel::openPlaylistBottomSheetIfTrackExists)
                    }

                    if (isSheetVisible) {
                        ModalBottomSheet(
                            onDismissRequest = { viewModel.hideBottomSheet() },
                            sheetState = sheetState
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    stringResource(R.string.add_contact),
                                    style = MaterialTheme.typography.titleLarge
                                )

                                Spacer(Modifier.size(80.dp))

                                OutlinedTextField(
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.inversePrimary,
                                        focusedContainerColor = MaterialTheme.colorScheme.inversePrimary,
                                        cursorColor = Color.White,
                                        focusedPlaceholderColor = Color.Transparent,
                                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    shape = MaterialTheme.shapes.extraLarge,
                                    placeholder = {
                                        Text(stringResource(R.string.username))
                                    },
                                    value = viewModel.addUserTextField.value,
                                    onValueChange = {
                                        viewModel.addUserTextField.value = it
                                    }
                                )

                                Spacer(Modifier.size(DPExtraLarge))
                                FloatingActionButton(
                                    shape = CircleShape,
                                    containerColor = if (isSystemInDarkTheme())
                                        MaterialTheme.colorScheme.primaryContainer
                                    else Color.Black,
                                    contentColor = Color.White,
                                    onClick = {
                                        viewModel.onAddNewUserBtnClicked(platformUiEffect)
                                    }
                                ) {
                                    Icon(
                                        imageVector = AppIcons.KeyboardArrowRight,
                                        contentDescription = null,
                                    )
                                }
                                Spacer(Modifier.size(80.dp))

                            }

                        }
                    }
                }
            }
        }


    }
}
