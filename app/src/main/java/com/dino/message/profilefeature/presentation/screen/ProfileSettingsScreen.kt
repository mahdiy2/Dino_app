package com.dino.message.profilefeature.presentation.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dino.message.R
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.backIconModifier
import com.dino.message.profilefeature.presentation.viewmodel.ProfileSettingsViewModel
import com.dino.message.threadfeature.presentation.component.PostContainer
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ProfileSettingsScreen() {
    val viewModel: ProfileSettingsViewModel = hiltViewModel()
    val showDeleteAccountDialog by viewModel.showDeleteAccountDialog.collectAsState()
    val deleteAccountLoading by viewModel.deleteAccountLoading.collectAsState()
    val deleteAccountError by viewModel.deleteAccountError.collectAsState()
    val logoutLoading by viewModel.logoutLoading.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState(initial = AppLanguage.Farsi)
    var showTermsDialog by rememberSaveable { mutableStateOf(false) }
    var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }

    if (showTermsDialog) {
        LegalTextDialogFa(
            title = stringResource(R.string.terms_and_conditions),
            content = TERMS_AND_CONDITIONS_FA,
            onDismissRequest = { showTermsDialog = false },
        )
    }

    if (showPrivacyDialog) {
        LegalTextDialogFa(
            title = stringResource(R.string.privacy_policy),
            content = PRIVACY_POLICY_FA,
            onDismissRequest = { showPrivacyDialog = false },
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteAccountDialog,
            title = { Text(stringResource(R.string.delete_account_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(DPMedium)) {
                    Text(text = stringResource(R.string.delete_account_body))
                    deleteAccountError?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = viewModel::dismissDeleteAccountDialog,
                    enabled = !deleteAccountLoading,
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDeleteAccount,
                    enabled = !deleteAccountLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    if (deleteAccountLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = DPMedium / 4,
                            color = MaterialTheme.colorScheme.onError,
                        )
                    } else {
                        Text(stringResource(R.string.delete_account))
                    }
                }
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = viewModel::navigateUp) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back),
                            modifier = backIconModifier(),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SettingsAnimatedBackdrop(modifier = Modifier.fillMaxSize())

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = DPExtraLarge,
                    vertical = DPLarge,
                ),
                verticalArrangement = Arrangement.spacedBy(DPLarge),
            ) {
                item("hero") {
                    AnimatedSection(delayMillis = 40) {
                        SettingsHeroCard(
                            currentLanguage = currentLanguage,
                            onToggleLanguage = {
                                val nextLanguage = if (currentLanguage == AppLanguage.English) {
                                    AppLanguage.Farsi
                                } else {
                                    AppLanguage.English
                                }
                                viewModel.setLanguage(nextLanguage)
                            },
                        )
                    }
                }

                item("privacy_header") {
                    AnimatedSection(delayMillis = 80) {
                        SettingsSectionHeader(
                            title = stringResource(R.string.privacy),
                            icon = Icons.Rounded.Block,
                        )
                    }
                }

                item("block_list") {
                    AnimatedSection(delayMillis = 115) {
                        SettingsActionCard(
                            title = stringResource(R.string.block_list),
                            subtitle = stringResource(R.string.block_list_description),
                            onClick = viewModel::openBlockList,
                            icon = { tint ->
                                Icon(
                                    imageVector = Icons.Rounded.Block,
                                    contentDescription = null,
                                    tint = tint,
                                )
                            },
                        )
                    }
                }

                item("encryption_fix") {
                    AnimatedSection(delayMillis = 150) {
                        SettingsActionCard(
                            title = stringResource(R.string.fix_message_encryption),
                            subtitle = stringResource(R.string.fix_message_encryption_description),
                            onClick = viewModel::openPrivateKeyResetDialog,
                            icon = { tint ->
                                Icon(
                                    painter = painterResource(R.drawable.baseline_vpn_key_24),
                                    contentDescription = null,
                                    tint = tint,
                                )
                            },
                        )
                    }
                }

                item("legal_header") {
                    AnimatedSection(delayMillis = 190) {
                        SettingsSectionHeader(
                            title = stringResource(R.string.legal),
                            icon = Icons.Rounded.Forum,
                        )
                    }
                }

                item("terms") {
                    AnimatedSection(delayMillis = 225) {
                        SettingsActionCard(
                            title = stringResource(R.string.terms_and_conditions),
                            subtitle = stringResource(R.string.terms_and_conditions_description),
                            onClick = { showTermsDialog = true },
                            icon = { tint ->
                                Icon(
                                    imageVector = Icons.Rounded.Forum,
                                    contentDescription = null,
                                    tint = tint,
                                )
                            },
                        )
                    }
                }

                item("privacy_policy") {
                    AnimatedSection(delayMillis = 260) {
                        SettingsActionCard(
                            title = stringResource(R.string.privacy_policy),
                            subtitle = stringResource(R.string.privacy_policy_description),
                            onClick = { showPrivacyDialog = true },
                            icon = { tint ->
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = null,
                                    tint = tint,
                                )
                            },
                        )
                    }
                }

                item("about_header") {
                    AnimatedSection(delayMillis = 300) {
                        SettingsSectionHeader(
                            title = stringResource(R.string.about),
                            icon = Icons.Rounded.Info,
                        )
                    }
                }

                item("about_us") {
                    AnimatedSection(delayMillis = 335) {
                        SettingsActionCard(
                            title = stringResource(R.string.about_us),
                            subtitle = stringResource(R.string.about_us_description),
                            onClick = viewModel::openAboutUs,
                            icon = { tint ->
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = tint,
                                )
                            },
                        )
                    }
                }

                item("account_header") {
                    AnimatedSection(delayMillis = 375) {
                        SettingsSectionHeader(
                            title = stringResource(R.string.account),
                            icon = Icons.Rounded.Person,
                        )
                    }
                }

                item("logout") {
                    AnimatedSection(delayMillis = 410) {
                        SettingsActionCard(
                            title = stringResource(R.string.log_out),
                            subtitle = stringResource(R.string.log_out_description),
                            onClick = viewModel::logout,
                            enabled = !logoutLoading && !deleteAccountLoading,
                            isBusy = logoutLoading,
                            icon = { tint ->
                                Icon(
                                    painter = painterResource(R.drawable.round_logout_24),
                                    contentDescription = null,
                                    tint = tint,
                                )
                            },
                        )
                    }
                }

                item("delete_account") {
                    AnimatedSection(delayMillis = 445) {
                        SettingsActionCard(
                            title = stringResource(R.string.delete_account),
                            subtitle = stringResource(R.string.delete_account_description),
                            onClick = viewModel::openDeleteAccountDialog,
                            enabled = !logoutLoading && !deleteAccountLoading,
                            isDestructive = true,
                            icon = { tint ->
                                Icon(
                                    imageVector = Icons.Rounded.DeleteForever,
                                    contentDescription = null,
                                    tint = tint,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsAnimatedBackdrop(modifier: Modifier = Modifier) {
    val primaryTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val secondaryTint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.07f)
    val transition = rememberInfiniteTransition(label = "settings_background")
    val firstBlobProgress by transition.animateFloat(
        initialValue = 0.05f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "settings_blob_1",
    )
    val secondBlobProgress by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = -0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "settings_blob_2",
    )

    Canvas(modifier = modifier) {
        drawCircle(
            color = primaryTint,
            radius = size.minDimension * 0.44f,
            center = Offset(
                x = size.width * firstBlobProgress,
                y = size.height * 0.12f,
            ),
        )
        drawCircle(
            color = secondaryTint,
            radius = size.minDimension * 0.36f,
            center = Offset(
                x = size.width * secondBlobProgress,
                y = size.height * 0.86f,
            ),
        )
    }
}

@Composable
private fun AnimatedSection(
    delayMillis: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "settings_section_alpha",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 22f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "settings_section_offset",
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offsetY
        },
    ) {
        content()
    }
}

@Composable
private fun SettingsHeroCard(
    currentLanguage: AppLanguage,
    onToggleLanguage: () -> Unit,
) {
    val languageLabel = if (currentLanguage == AppLanguage.English) {
        stringResource(R.string.english_short)
    } else {
        stringResource(R.string.persian_short)
    }

    PostContainer(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.76f),
                        ),
                    ),
                )
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(DPMedium)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DPMedium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                    ) {
                        Icon(
                            modifier = Modifier.padding(DPMedium),
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }

                Text(
                    text = stringResource(R.string.message_privacy_title),
                    style = MaterialTheme.typography.bodyLarge,
                )

                SettingsInfoPill(
                    text = "${stringResource(R.string.app_language)}: $languageLabel",
                    onClick = onToggleLanguage,
                )
            }
        }
    }
}

@Composable
private fun SettingsInfoPill(
    text: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(),
        label = "settings_language_toggle_scale",
    )

    Surface(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(MaterialTheme.shapes.large)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DPLarge, vertical = DPMedium),
            horizontalArrangement = Arrangement.spacedBy(DPSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = Icons.Rounded.Language,
                contentDescription = null,
            )
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: ImageVector,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DPMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Icon(
                modifier = Modifier.padding(7.dp),
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SettingsActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: @Composable (tint: Color) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isBusy: Boolean = false,
    isDestructive: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.98f else 1f,
        animationSpec = spring(),
        label = "settings_action_scale",
    )

    val titleColor = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val iconTint = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val iconContainerColor = if (isDestructive) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    PostContainer(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else 0.62f)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (pressed && enabled) 5.dp else 9.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DPLarge, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(DPMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = iconContainerColor,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    icon(iconTint)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DPSmall),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = DPMedium / 4,
                )
            }
        }
    }
}

@Composable
private fun LegalTextDialogFa(
    title: String,
    content: String,
    onDismissRequest: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = title) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = onDismissRequest) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

private val TERMS_AND_CONDITIONS_FA = """
به اپلیکیشن Dino خوش آمدید. با استفاده از این برنامه، شما این شرایط را می‌پذیرید:

۱) مسئولیت حساب کاربری
شما مسئول نگهداری اطلاعات ورود خود هستید. نام کاربری باید معتبر باشد و استفاده از حساب دیگران مجاز نیست.

۲) امنیت پیام‌ها
پیام‌های خصوصی و گروهی با سازوکارهای امنیتی برنامه محافظت می‌شوند. با این حال، شما مسئول امنیت دستگاه و اطلاعات ورود خود هستید.

۳) محتوای مجاز
ارسال محتوای کلاهبرداری، خشونت‌آمیز، اسپم، غیراخلاقی یا اطلاعات نادرست مجاز نیست. کاربران می‌توانند محتوا را گزارش کنند و تیم مدیریت می‌تواند محتوا یا حساب را محدود/حذف کند.

۴) روابط کاربران
امکان بلاک‌کردن کاربران وجود دارد. در صورت وجود بلاک دوطرفه یا یک‌طرفه، برخی قابلیت‌ها مانند پیام‌رسانی محدود می‌شود.

۵) محدودیت‌های فنی
برای پایداری سرویس، محدودیت‌هایی مانند حجم فایل‌های رسانه‌ای، نوع فایل‌ها، تعداد گروه‌های قابل ساخت و صفحه‌بندی داده‌ها اعمال می‌شود.

۶) حذف حساب
با حذف حساب از بخش تنظیمات، داده‌های مرتبط با حساب شما به‌صورت دائمی از سامانه حذف می‌شود و این عمل قابل بازگشت نیست.

۷) رعایت قوانین جمهوری اسلامی ایران
استفاده از این برنامه باید مطابق قوانین و مقررات جمهوری اسلامی ایران باشد. هرگونه استفاده مغایر با قوانین کشور، از جمله انتشار محتوای مجرمانه، ممنوع است و می‌تواند منجر به محدودسازی یا حذف حساب شود.

۸) تغییرات سرویس
ممکن است قابلیت‌ها، سیاست‌های مدیریت محتوا یا محدودیت‌های فنی در نسخه‌های بعدی تغییر کنند. ادامه استفاده از برنامه به‌معنای پذیرش نسخه به‌روز شرایط است.
""".trimIndent()

private val PRIVACY_POLICY_FA = """
این سیاست توضیح می‌دهد در Dino چه داده‌هایی پردازش می‌شود و چگونه استفاده می‌شود:

۱) داده‌های حساب
برای ایجاد حساب، نام کاربری، رمز عبور هش‌شده، کلید عمومی و اطلاعات پروفایل (نام، بیو، تصویر/ویدئو پروفایل) ذخیره می‌شود.

۲) داده‌های پیام و تعامل
پیام‌ها، شناسه پیام، زمان ارسال، وضعیت خوانده‌شدن، پاسخ به پیام، فایل‌های ضمیمه و همچنین تعاملاتی مثل دنبال‌کردن، بلاک، رأی، کامنت و گزارش محتوا پردازش می‌شوند.

۳) نگه‌داری پیام‌ها در سرور
تاریخچه پیام‌ها برای ارائه قابلیت‌هایی مانند همگام‌سازی بین دستگاه‌ها، بازیابی گفتگوها و نمایش سابقه چت روی سرور نگه‌داری می‌شود. این داده‌ها با تدابیر امنیتی مناسب محافظت می‌شوند و دسترسی به آن‌ها محدود است. کاربران می‌توانند مطابق امکانات برنامه، پیام‌ها یا حساب خود را حذف کنند.

۴) داده‌های فنی و خطا
برای رفع اشکال، لاگ خطا شامل نوع خطا، پشته خطا، نسخه برنامه، اطلاعات دستگاه و در صورت وجود، شناسه کاربر ثبت می‌شود.

۵) محل و شیوه نگهداری
داده‌ها متناسب با نیاز سرویس، به‌صورت محدود در سرور و دستگاه کاربر نگهداری می‌شوند تا عملکرد برنامه و امنیت حساب فراهم شود.

۶) استفاده از داده‌ها
داده‌ها برای ارائه پیام‌رسانی، نمایش محتوا و پروفایل، اعلان‌ها، مدیریت مخاطبین/گروه‌ها، امنیت حساب و مدیریت گزارش‌های تخلف استفاده می‌شوند.

۷) اشتراک‌گذاری داده
داده‌ها برای عملکرد هسته سرویس استفاده می‌شوند و فقط در حد لازم برای تحویل پیام، نمایش رسانه و مدیریت سیستم پردازش می‌گردند.

۸) حقوق کاربر
شما می‌توانید حساب خود را حذف کنید. با حذف حساب، داده‌های وابسته به آن طبق منطق حذف سامانه پاک می‌شود.

۹) امنیت
برای حفاظت از حساب‌ها و داده‌ها از تدابیر امنیتی مناسب استفاده می‌شود. با این حال هیچ سامانه‌ای امنیت مطلق را تضمین نمی‌کند و کاربر نیز باید از دستگاه خود محافظت کند.
""".trimIndent()
