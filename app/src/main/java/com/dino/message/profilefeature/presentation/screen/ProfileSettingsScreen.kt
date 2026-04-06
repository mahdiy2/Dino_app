package com.dino.message.profilefeature.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.dino.message.R
import com.dino.message.corefeature.presentation.ui.theme.DPLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.profilefeature.presentation.viewmodel.ProfileSettingsViewModel
import com.dino.message.threadfeature.presentation.component.PostContainer
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ProfileSettingsScreen() {
    val viewModel: ProfileSettingsViewModel = hiltViewModel()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = viewModel::navigateUp) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = DPLarge, vertical = DPMedium),
            verticalArrangement = Arrangement.spacedBy(DPMedium)
        ) {
            Text(
                text = "Privacy",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PostContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = viewModel::openBlockList)
            ) {
                Column(
                    modifier = Modifier.padding(DPLarge),
                    verticalArrangement = Arrangement.spacedBy(DPMedium)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = null
                    )
                    Text(
                        text = "Block List",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Manage users you have blocked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PostContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = viewModel::openPrivateKeyResetDialog)
            ) {
                Column(
                    modifier = Modifier.padding(DPLarge),
                    verticalArrangement = Arrangement.spacedBy(DPMedium)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_vpn_key_24),
                        contentDescription = null
                    )
                    Text(
                        text = "Fix Message Encryption",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Reset keys if messages can’t decrypt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
