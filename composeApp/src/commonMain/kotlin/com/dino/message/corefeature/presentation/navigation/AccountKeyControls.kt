package com.dino.message.corefeature.presentation.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dino.message.authfeature.domain.model.AccountKeyRecord
import com.dino.message.authfeature.presentation.screen.RegisterContent
import com.dino.message.corefeature.domain.model.AppLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun AccountKeyControls(
    keys: AccountKeyWorkflow,
    unlocked: AccountKeyRecord?,
    onUnlock: (AccountKeyRecord?) -> Unit,
    onSessionExpired: () -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var backup by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var staged by remember { mutableStateOf<AccountKeyRecord?>(null) }
    var acknowledged by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(if (unlocked == null) "Messaging key is locked or missing." else "Messaging key is unlocked.") }
    val scope = rememberCoroutineScope()
    fun run(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: ShellSessionExpiredException) {
                onSessionExpired()
            } catch (_: Exception) {
                status = "Could not complete the key operation. Check the backup, passphrase, or connection."
            } finally {
                busy = false
            }
        }
    }
    val canOpen = !busy && (!keys.needsPassphrase || passphrase.length >= 12)
    Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
        Text(status)
        if (unlocked != null) {
            TextButton(onClick = {
                onUnlock(null)
                staged = null
                passphrase = ""
                status = "Messaging key is locked."
            }) { Text("Lock key") }
        } else {
            if (keys.needsPassphrase) SecretField("Vault passphrase (at least 12 characters)", passphrase, { passphrase = it })
            Button(enabled = canOpen, onClick = {
                run {
                    val key = keys.open(passphrase)
                    onUnlock(key)
                    status = if (key == null) "No matching backup found." else "Messaging key is unlocked."
                    if (key != null) passphrase = ""
                }
            }) { Text("Unlock key") }
            SecretField("Private-key backup", backup, { backup = it })
            Button(enabled = canOpen && backup.isNotBlank(), onClick = {
                run {
                    onUnlock(keys.importBackup(passphrase, backup.trim()))
                    backup = ""
                    passphrase = ""
                    status = "Messaging key is unlocked."
                }
            }) { Text("Import backup") }
            TextButton(enabled = canOpen, onClick = { run { staged = keys.prepareReset(passphrase); acknowledged = false } }) {
                Text("Prepare a new messaging key")
            }
            staged?.let { record ->
                Text("Replacing your key may make older encrypted messages unreadable.")
                BackupAcknowledgment(record.privateKey, acknowledged, { acknowledged = it })
                SecretField("Account password", password, { password = it })
                Button(enabled = canOpen && acknowledged && password.isNotEmpty(), onClick = {
                    run {
                        try {
                            onUnlock(keys.replaceKey(passphrase, password))
                            staged = null
                            passphrase = ""
                            status = "Messaging key was replaced."
                        } finally {
                            password = ""
                        }
                    }
                }) { Text("Replace key") }
            }
        }
    }
}

@Composable
internal fun RegistrationControls(
    keys: AccountKeyWorkflow,
    onBack: () -> Unit,
    onSignIn: (String, String) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var staged by remember { mutableStateOf<AccountKeyRecord?>(null) }
    var acknowledged by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    fun run(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                status = "Registration could not be completed. Verify the details and retry."
            } finally {
                busy = false
            }
        }
    }

    val record = staged
    if (record == null) {
        RegisterContent(
            username = username,
            password = password,
            referralCode = referralCode,
            registerErrorMessage = status.ifEmpty { null },
            registerLoading = busy,
            currentLanguage = AppLanguage.English,
            isUsernameFormatValid = { it.matches(Regex("[A-Za-z0-9._-]+")) },
            onUsernameChange = { username = it },
            onPasswordChange = { password = it },
            onReferralCodeChange = { referralCode = it },
            onRegisterClick = {
                if (keys.needsPassphrase && passphrase.length < 12) {
                    status = "Use a vault passphrase of at least 12 characters."
                } else {
                    run {
                        staged = keys.beginRegistration(username.trim(), password, passphrase, referralCode.trim().ifBlank { null })
                    }
                }
            },
            onAlreadyHaveAccountClick = onBack,
            onToggleLanguage = {},
            showLanguageToggle = false,
            extraFields = {
                if (keys.needsPassphrase) SecretField("Vault passphrase", passphrase, { passphrase = it })
            },
        )
        return
    }
    Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState())) {
        Text("Confirm registration")
        if (status.isNotEmpty()) Text(status)
        BackupAcknowledgment(record.privateKey, acknowledged, { acknowledged = it })
        Button(enabled = !busy && acknowledged && password.isNotEmpty(), onClick = {
            run {
                when (keys.confirmRegistration(username.trim(), passphrase)) {
                    in 200..299, 404, 409 -> onSignIn(username.trim(), password)
                    410 -> {
                        staged = null
                        acknowledged = false
                        status = "Registration expired. Start again."
                    }
                    else -> status = "Registration could not be confirmed. Check the account and try again."
                }
            }
        }) { Text("Confirm registration") }
        TextButton(onClick = onBack, enabled = !busy) { Text("Back to sign in") }
    }
}

@Composable
private fun SecretField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation())
}

@Composable
private fun BackupAcknowledgment(privateKey: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    val clipboard = LocalClipboardManager.current
    TextButton(onClick = { clipboard.setText(AnnotatedString(privateKey)) }) { Text("Copy private-key backup") }
    Text("I saved the backup somewhere safe.")
    Checkbox(checked = checked, onCheckedChange = onChecked)
}
