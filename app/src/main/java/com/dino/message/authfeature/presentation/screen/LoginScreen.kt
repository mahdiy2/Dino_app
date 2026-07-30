package com.dino.message.authfeature.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.dino.message.R
import com.dino.message.authfeature.presentation.viewmodel.LoginScreenViewModel
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPTopAppBar
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph


@Destination<RootGraph>
@Composable
fun LoginScreen() {

    val viewModel: LoginScreenViewModel = koinViewModel()
    val loginErrorMessage by viewModel.loginErrorMessage
    val currentLanguage by viewModel.currentLanguage.collectAsState(initial = AppLanguage.Farsi)

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            IconButton(
                modifier = Modifier
                    .align(AbsoluteAlignment.TopRight)
                    .statusBarsPadding()
                    .padding(DPMedium),
                onClick = viewModel::toggleLanguage
            ) {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = if (currentLanguage == AppLanguage.Farsi) {
                        stringResource(R.string.switch_to_english)
                    } else {
                        stringResource(R.string.switch_to_persian)
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Spacer(Modifier.size(DPTopAppBar))
                    Text(
                        stringResource(R.string.login),
                        fontWeight = FontWeight.Bold,
                        fontSize = 50.sp,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = DPMedium),
                ) {
                    OutlinedTextField(
                        isError = viewModel.usernameTextFieldIsError.value,
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
                        value = viewModel.userName.value,
                        onValueChange = { viewModel.usernameOnValueChange(it) },
                        placeholder = {
                            Text(stringResource(R.string.username))
                        },
                        maxLines = 1
                    )

                    Spacer(
                        Modifier.size(DPExtraLarge)
                    )

                    OutlinedTextField(
                        isError = viewModel.passwordTextFieldIsError.value,
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
                        value = viewModel.password.value,
                        onValueChange = { viewModel.passwordOnValueChange(it) },
                        placeholder = {
                            Text(stringResource(R.string.password))
                        },
                        maxLines = 1
                    )

                    Spacer(
                        Modifier.size(DPExtraLarge)
                    )

                    if (!loginErrorMessage.isNullOrBlank()) {
                        Text(
                            text = loginErrorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    AnimatedVisibility(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally),
                        visible = true
                    ) {
                        Column {
                            Spacer(
                                Modifier.size(DPExtraLarge)
                            )

                            FloatingActionButton(
                                shape = CircleShape,
                                containerColor = if (isSystemInDarkTheme())
                                    MaterialTheme.colorScheme.primaryContainer
                                else Color.Black,
                                contentColor = Color.White,
                                onClick = {
                                    viewModel.onLoginBtnClicked()
                                }
                            ) {
                                if (viewModel.loginLoadingState.value){
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = stringResource(R.string.login),
                                    )
                                }

                            }
                        }
                    }

                    Spacer(Modifier.size(DPTopAppBar))

                }

                TextButton(
                    onClick = {
                        viewModel.navigateToRegisterScreen()
                    }
                ) {
                    Text(stringResource(R.string.create_account))
                }
            }
        }

    }
}
