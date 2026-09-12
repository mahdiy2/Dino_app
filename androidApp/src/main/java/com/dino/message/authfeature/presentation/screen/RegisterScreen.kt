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
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import com.dino.message.R
import com.dino.message.authfeature.presentation.viewmodel.RegisterScreenViewModel
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPTopAppBar
import com.dino.message.corefeature.presentation.icons.AppIcons


@Composable
fun RegisterScreen() {

    val viewModel: RegisterScreenViewModel = rememberStateHolder()
    val registerErrorMessage by viewModel.registerErrorMessage
    val currentLanguage by viewModel.currentLanguage.collectAsState(initial = AppLanguage.Farsi)
    val minUsernameLength = 7
    val minPasswordLength = 10
    val trimmedUsername = viewModel.userName.value.trim()
    val usernameTooShort = viewModel.userName.value.isNotEmpty() &&
        trimmedUsername.length < minUsernameLength
    val usernameHasInvalidChars = viewModel.userName.value.isNotEmpty() &&
        !viewModel.isUsernameFormatValid(trimmedUsername)

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
                    imageVector = AppIcons.Language,
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
                        stringResource(R.string.register),
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
                        onValueChange = { viewModel.onUsernameValueChange(it) },
                        isError = usernameTooShort || usernameHasInvalidChars,
                        placeholder = {
                            Text(stringResource(R.string.username))
                        },
                        supportingText = {
                            if (usernameHasInvalidChars) {
                                Text(stringResource(R.string.username_allowed_chars))
                            } else {
                                Text(
                                    text = stringResource(
                                        R.string.minimum_chars_count,
                                        minUsernameLength,
                                        trimmedUsername.length,
                                        minUsernameLength
                                    )
                                )
                            }
                        },
                        maxLines = 1
                    )

                    Spacer(
                        Modifier.size(DPExtraLarge)
                    )

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
                        value = viewModel.password.value,
                        onValueChange = { viewModel.onPasswordValueChange(it) },
                        isError = viewModel.password.value.isNotEmpty() &&
                            viewModel.password.value.length < minPasswordLength,
                        placeholder = {
                            Text(stringResource(R.string.password))
                        },
                        supportingText = {
                            Text(
                                text = stringResource(
                                    R.string.minimum_chars_count,
                                    minPasswordLength,
                                    viewModel.password.value.length,
                                    minPasswordLength
                                )
                            )
                        },
                        maxLines = 1
                    )

                    Text(
                        text = stringResource(R.string.encryption_keys_auto_generated),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(
                        Modifier.size(DPMedium)
                    )

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
                        value = viewModel.referralCode.value,
                        onValueChange = viewModel::onReferralCodeValueChange,
                        placeholder = {
                            Text(stringResource(R.string.referral_code_optional))
                        },
                        supportingText = {
                            Text(stringResource(R.string.referral_code_optional_hint))
                        },
                        maxLines = 1
                    )

                    AnimatedVisibility(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally),
                        visible = (
                            trimmedUsername.length >= minUsernameLength &&
                                !usernameHasInvalidChars &&
                                viewModel.password.value.length >= minPasswordLength
                            )
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
                                    viewModel.onClickBtnRegister()
                                }
                            ) {
                                if (viewModel.registerLoadingState.value){
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Icon(
                                        imageVector = AppIcons.KeyboardArrowRight,
                                        contentDescription = stringResource(R.string.register),
                                    )
                                }
                            }

                            if (!registerErrorMessage.isNullOrBlank()) {
                                Spacer(Modifier.size(DPMedium))
                                Text(
                                    text = registerErrorMessage.orEmpty(),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(Modifier.size(DPTopAppBar))

                }

                TextButton(
                    onClick = {
                        viewModel.navigateToLoginScreen()
                    }
                ) {
                    Text(stringResource(R.string.already_have_account))
                }
            }
        }

    }
}
