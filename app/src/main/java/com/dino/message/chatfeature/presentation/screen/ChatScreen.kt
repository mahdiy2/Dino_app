package com.dino.message.chatfeature.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dino.message.chatfeature.presentation.component.ChatScreenBottomAppBar
import com.dino.message.chatfeature.presentation.component.ChatSurface
import com.dino.message.chatfeature.presentation.viewmodel.ChatViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ChatScreen(chatId: String) {
    val viewModel: ChatViewModel = hiltViewModel()
    val isPeerOnline by viewModel.isPeerOnline.collectAsState()
    val socketDebugEvents by viewModel.socketDebugEvents.collectAsState()


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "$chatId • ${if (isPeerOnline) "Online" else "Offline"}")
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.clearMessages()
                        }
                    ) {
                        Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Clear")
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.navigateUp()
                        }
                    ) {
                        Icon(imageVector = Icons.Rounded.ArrowBackIosNew, contentDescription = "Back Button")
                    }
                }
            )
        },
        bottomBar = {
            ChatScreenBottomAppBar() {
                if (it.isNotEmpty()) {
                    viewModel.sendMessage(it)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (socketDebugEvents.isNotEmpty()) {
                Text(
                    text = "Socket Debug (latest ${'$'}{socketDebugEvents.size})",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Text(
                    text = socketDebugEvents.takeLast(5).joinToString("\n"),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            Box(modifier = Modifier.fillMaxSize()) {
                ChatSurface(
                    modifier = Modifier
                        .fillMaxSize(),
                    items = viewModel.messages,
                    sender = chatId
                )
            }
        }
    }
}
