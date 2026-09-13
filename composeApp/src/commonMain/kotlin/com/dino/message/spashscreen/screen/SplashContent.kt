package com.dino.message.spashscreen.screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dino.message.composeapp.generated.resources.Res
import com.dino.message.composeapp.generated.resources.built_in_war
import com.dino.message.composeapp.generated.resources.english_short
import com.dino.message.composeapp.generated.resources.ic_dino
import com.dino.message.composeapp.generated.resources.persian_short
import com.dino.message.composeapp.generated.resources.retry
import com.dino.message.composeapp.generated.resources.splash_checking_version
import com.dino.message.composeapp.generated.resources.splash_language_prompt
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.presentation.resource.asComposeString
import com.dino.message.spashscreen.viewmodel.SplashUiState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SplashContent(
    uiState: SplashUiState,
    onLanguageSelected: (AppLanguage) -> Unit,
    onRetryVersionCheck: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val animationCoefficient by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = TweenSpec(durationMillis = 3_000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logo_scale",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_dino),
            contentDescription = null,
            modifier = Modifier
                .size(180.dp)
                .scale(animationCoefficient),
            tint = Color.Unspecified,
        )

        if (uiState.showLanguageSelection) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.splash_language_prompt),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { onLanguageSelected(AppLanguage.Farsi) },
                            enabled = !uiState.isSavingLanguage,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = stringResource(Res.string.persian_short))
                        }
                        OutlinedButton(
                            onClick = { onLanguageSelected(AppLanguage.English) },
                            enabled = !uiState.isSavingLanguage,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = stringResource(Res.string.english_short))
                        }
                    }
                    if (uiState.isChecking) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                            )
                            Text(
                                text = stringResource(Res.string.splash_checking_version),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage.asComposeString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        OutlinedButton(
                            onClick = onRetryVersionCheck,
                            enabled = !uiState.isSavingLanguage,
                        ) {
                            Text(text = stringResource(Res.string.retry))
                        }
                    }
                }
            }
        } else if (uiState.isChecking) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                Text(
                    text = stringResource(Res.string.splash_checking_version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        } else if (uiState.errorMessage != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = uiState.errorMessage.asComposeString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onRetryVersionCheck) {
                    Text(text = stringResource(Res.string.retry))
                }
            }
        }

        Text(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars),
            text = stringResource(Res.string.built_in_war),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}
