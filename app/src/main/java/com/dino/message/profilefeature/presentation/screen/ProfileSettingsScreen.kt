package com.dino.message.profilefeature.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
    val showDeleteAccountDialog by viewModel.showDeleteAccountDialog.collectAsState()
    val deleteAccountLoading by viewModel.deleteAccountLoading.collectAsState()
    val deleteAccountError by viewModel.deleteAccountError.collectAsState()
    val logoutLoading by viewModel.logoutLoading.collectAsState()
    var showTermsDialog by rememberSaveable { mutableStateOf(false) }
    var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }

    if (showTermsDialog) {
        LegalTextDialogFa(
            title = "Terms & Conditions",
            content = TERMS_AND_CONDITIONS_FA,
            onDismissRequest = { showTermsDialog = false }
        )
    }

    if (showPrivacyDialog) {
        LegalTextDialogFa(
            title = "Privacy Policy",
            content = PRIVACY_POLICY_FA,
            onDismissRequest = { showPrivacyDialog = false }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteAccountDialog,
            title = { Text("Delete account?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(DPMedium)) {
                    Text(
                        "This action is permanent. Once your account is deleted, we do not keep any of your data on our servers - as if you never had an account in this app."
                    )
                    deleteAccountError?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = viewModel::dismissDeleteAccountDialog,
                    enabled = !deleteAccountLoading
                ) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDeleteAccount,
                    enabled = !deleteAccountLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    if (deleteAccountLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = DPMedium / 4,
                            color = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        Text("Delete Account")
                    }
                }
            }
        )
    }

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

            Text(
                text = "Legal",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PostContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { showTermsDialog = true })
            ) {
                Column(
                    modifier = Modifier.padding(DPLarge),
                    verticalArrangement = Arrangement.spacedBy(DPMedium)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Forum,
                        contentDescription = null
                    )
                    Text(
                        text = "Terms & Conditions",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Read terms for using the app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PostContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { showPrivacyDialog = true })
            ) {
                Column(
                    modifier = Modifier.padding(DPLarge),
                    verticalArrangement = Arrangement.spacedBy(DPMedium)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null
                    )
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Read how data is collected and used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PostContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !logoutLoading && !deleteAccountLoading, onClick = viewModel::logout)
            ) {
                Column(
                    modifier = Modifier.padding(DPLarge),
                    verticalArrangement = Arrangement.spacedBy(DPMedium)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.round_logout_24),
                        contentDescription = null
                    )
                    Text(
                        text = "Log Out",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Sign out and clear local app data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (logoutLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = DPMedium / 4
                        )
                    }
                }
            }

            PostContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = !logoutLoading && !deleteAccountLoading,
                        onClick = viewModel::openDeleteAccountDialog
                    )
            ) {
                Column(
                    modifier = Modifier.padding(DPLarge),
                    verticalArrangement = Arrangement.spacedBy(DPMedium)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Delete Account",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Delete your account and remove all your data permanently",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                    Text("Close")
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
پیام‌های کاربران به‌صورت دائمی روی سرور ذخیره نمی‌شوند. پیام‌ها فقط تا زمان تحویل به گیرنده نگه‌داری موقت می‌شوند و پس از تحویل حذف می‌گردند. همچنین اگر پیام توسط کاربر طی ۲۴ ساعت خوانده نشود، برای افزایش امنیت از سرور حذف خواهد شد.

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
