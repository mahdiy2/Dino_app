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
import com.dino.message.composeapp.generated.resources.create_account
import com.dino.message.composeapp.generated.resources.login
import com.dino.message.composeapp.generated.resources.password
import com.dino.message.composeapp.generated.resources.switch_to_english
import com.dino.message.composeapp.generated.resources.switch_to_persian
import com.dino.message.composeapp.generated.resources.username
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.presentation.icons.AppIcons
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPTopAppBar
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginContent(
    username: String,
    password: String,
    usernameIsError: Boolean,
    passwordIsError: Boolean,
    loginErrorMessage: String?,
    loginLoading: Boolean,
    currentLanguage: AppLanguage,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onToggleLanguage: () -> Unit,
    showLanguageToggle: Boolean = true,
) {
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
                        stringResource(Res.string.login),
                        fontWeight = FontWeight.Bold,
                        fontSize = 50.sp,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(modifier = Modifier.padding(horizontal = DPMedium)) {
                    OutlinedTextField(
                        isError = usernameIsError,
                        colors = authTextFieldColors(),
                        shape = MaterialTheme.shapes.extraLarge,
                        value = username,
                        onValueChange = onUsernameChange,
                        placeholder = { Text(stringResource(Res.string.username)) },
                        maxLines = 1
                    )

                    Spacer(Modifier.size(DPExtraLarge))

                    OutlinedTextField(
                        isError = passwordIsError,
                        colors = authTextFieldColors(),
                        shape = MaterialTheme.shapes.extraLarge,
                        value = password,
                        visualTransformation = PasswordVisualTransformation(),
                        onValueChange = onPasswordChange,
                        placeholder = { Text(stringResource(Res.string.password)) },
                        maxLines = 1
                    )

                    Spacer(Modifier.size(DPExtraLarge))

                    if (!loginErrorMessage.isNullOrBlank()) {
                        Text(
                            text = loginErrorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    AnimatedVisibility(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        visible = true
                    ) {
                        Column {
                            Spacer(Modifier.size(DPExtraLarge))
                            FloatingActionButton(
                                shape = CircleShape,
                                containerColor = if (isSystemInDarkTheme())
                                    MaterialTheme.colorScheme.primaryContainer
                                else Color.Black,
                                contentColor = Color.White,
                                onClick = onLoginClick
                            ) {
                                if (loginLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Icon(
                                        imageVector = AppIcons.KeyboardArrowRight,
                                        contentDescription = stringResource(Res.string.login),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.size(DPTopAppBar))
                }

                TextButton(onClick = onCreateAccountClick) {
                    Text(stringResource(Res.string.create_account))
                }
            }
        }
    }
}

@Composable
internal fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = MaterialTheme.colorScheme.inversePrimary,
    focusedContainerColor = MaterialTheme.colorScheme.inversePrimary,
    cursorColor = Color.White,
    focusedPlaceholderColor = Color.Transparent,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.secondary
)
