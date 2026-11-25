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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dino.message.composeapp.generated.resources.Res
import com.dino.message.composeapp.generated.resources.already_have_account
import com.dino.message.composeapp.generated.resources.encryption_keys_auto_generated
import com.dino.message.composeapp.generated.resources.minimum_chars_count
import com.dino.message.composeapp.generated.resources.password
import com.dino.message.composeapp.generated.resources.referral_code_optional
import com.dino.message.composeapp.generated.resources.referral_code_optional_hint
import com.dino.message.composeapp.generated.resources.register
import com.dino.message.composeapp.generated.resources.switch_to_english
import com.dino.message.composeapp.generated.resources.switch_to_persian
import com.dino.message.composeapp.generated.resources.username
import com.dino.message.composeapp.generated.resources.username_allowed_chars
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.presentation.icons.AppIcons
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPTopAppBar
import org.jetbrains.compose.resources.stringResource

@Composable
fun RegisterContent(
    username: String,
    password: String,
    referralCode: String,
    registerErrorMessage: String?,
    registerLoading: Boolean,
    currentLanguage: AppLanguage,
    isUsernameFormatValid: (String) -> Boolean,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onReferralCodeChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onAlreadyHaveAccountClick: () -> Unit,
    onToggleLanguage: () -> Unit,
    showLanguageToggle: Boolean = true,
    extraFields: @Composable () -> Unit = {},
) {
    val minUsernameLength = 7
    val minPasswordLength = 10
    val trimmedUsername = username.trim()
    val usernameTooShort = username.isNotEmpty() && trimmedUsername.length < minUsernameLength
    val usernameHasInvalidChars = username.isNotEmpty() && !isUsernameFormatValid(trimmedUsername)

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (showLanguageToggle) IconButton(
                modifier = Modifier
                    .align(AbsoluteAlignment.TopRight)
                    .statusBarsPadding()
                    .padding(DPMedium),
                onClick = onToggleLanguage
            ) {
                Icon(
                    imageVector = AppIcons.Language,
                    contentDescription = if (currentLanguage == AppLanguage.Farsi) {
                        stringResource(Res.string.switch_to_english)
                    } else {
                        stringResource(Res.string.switch_to_persian)
                    }
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Spacer(Modifier.size(DPTopAppBar))
                    Text(
                        stringResource(Res.string.register),
                        fontWeight = FontWeight.Bold,
                        fontSize = 50.sp,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(modifier = Modifier.padding(horizontal = DPMedium)) {
                    OutlinedTextField(
                        colors = authTextFieldColors(),
                        shape = MaterialTheme.shapes.extraLarge,
                        value = username,
                        onValueChange = onUsernameChange,
                        isError = usernameTooShort || usernameHasInvalidChars,
                        placeholder = { Text(stringResource(Res.string.username)) },
                        supportingText = {
                            if (usernameHasInvalidChars) {
                                Text(stringResource(Res.string.username_allowed_chars))
                            } else {
                                Text(stringResource(Res.string.minimum_chars_count, minUsernameLength, trimmedUsername.length, minUsernameLength))
                            }
                        },
                        maxLines = 1
                    )

                    Spacer(Modifier.size(DPExtraLarge))

                    OutlinedTextField(
                        colors = authTextFieldColors(),
                        shape = MaterialTheme.shapes.extraLarge,
                        value = password,
                        visualTransformation = PasswordVisualTransformation(),
                        onValueChange = onPasswordChange,
                        isError = password.isNotEmpty() && password.length < minPasswordLength,
                        placeholder = { Text(stringResource(Res.string.password)) },
                        supportingText = {
                            Text(stringResource(Res.string.minimum_chars_count, minPasswordLength, password.length, minPasswordLength))
                        },
                        maxLines = 1
                    )

                    Text(
                        text = stringResource(Res.string.encryption_keys_auto_generated),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.size(DPMedium))

                    OutlinedTextField(
                        colors = authTextFieldColors(),
                        shape = MaterialTheme.shapes.extraLarge,
                        value = referralCode,
                        onValueChange = onReferralCodeChange,
                        placeholder = { Text(stringResource(Res.string.referral_code_optional)) },
                        supportingText = { Text(stringResource(Res.string.referral_code_optional_hint)) },
                        maxLines = 1
                    )

                    extraFields()

                    AnimatedVisibility(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        visible = trimmedUsername.length >= minUsernameLength &&
                            !usernameHasInvalidChars &&
                            password.length >= minPasswordLength
                    ) {
                        Column {
                            Spacer(Modifier.size(DPExtraLarge))
                            FloatingActionButton(
                                shape = CircleShape,
                                containerColor = if (isSystemInDarkTheme())
                                    MaterialTheme.colorScheme.primaryContainer
                                else Color.Black,
                                contentColor = Color.White,
                                onClick = onRegisterClick
                            ) {
                                if (registerLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Icon(
                                        imageVector = AppIcons.KeyboardArrowRight,
                                        contentDescription = stringResource(Res.string.register),
                                    )
                                }
                            }

                            if (!registerErrorMessage.isNullOrBlank()) {
                                Spacer(Modifier.size(DPMedium))
                                Text(
                                    text = registerErrorMessage,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(Modifier.size(DPTopAppBar))
                }

                TextButton(onClick = onAlreadyHaveAccountClick) {
                    Text(stringResource(Res.string.already_have_account))
                }
            }
        }
    }
}
