package com.dino.message.profilefeature.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dino.message.R
import com.dino.message.corefeature.data.upload.AndroidMediaOutbox
import com.dino.message.corefeature.domain.model.StringResourceContent
import com.dino.message.corefeature.domain.resource.StringResolver
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject

@Composable
internal fun MediaSubmissionsDialog(onDismiss: () -> Unit) {
    val outbox: AndroidMediaOutbox = koinInject()
    val resolver: StringResolver = koinInject()
    val entries by remember(outbox) { outbox.observeCurrentSubmissions() }.collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.media_submission_title)) },
        text = {
            if (entries.isEmpty()) {
                Text(stringResource(R.string.media_submission_empty))
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(entries, key = { it.id }) { row ->
                        val title = when (row.destination) {
                            "Private chat" -> R.string.media_submission_private
                            "Group chat" -> R.string.media_submission_group
                            "Profile" -> R.string.media_submission_profile
                            "Music post" -> R.string.media_submission_music
                            "Story" -> R.string.media_submission_story
                            else -> R.string.media_submission_post
                        }
                        val status = when (row.state) {
                            "pending" -> R.string.media_submission_pending
                            "complete" -> if (row.destination.endsWith("chat")) {
                                R.string.media_submission_handed_off
                            } else {
                                R.string.media_submission_complete
                            }
                            else -> R.string.media_submission_failed
                        }
                        Column {
                            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(status))
                            row.failure?.let {
                                Text(Json.decodeFromString<StringResourceContent>(it).resolveWith(resolver))
                            }
                            Row {
                                if (row.state == "retryable") {
                                    TextButton(onClick = { scope.launch { outbox.retry(row.id) } }) {
                                        Text(stringResource(R.string.media_submission_retry))
                                    }
                                }
                                TextButton(onClick = { scope.launch { outbox.remove(row.id) } }) {
                                    Text(stringResource(
                                        if (row.state == "pending") R.string.cancel
                                        else R.string.media_submission_dismiss
                                    ))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.media_submission_close)) }
        },
    )
}
