package com.dino.message.corefeature.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dino.message.composeapp.generated.resources.Res
import com.dino.message.composeapp.generated.resources.dialog_type_icon
import com.dino.message.composeapp.generated.resources.empty
import com.dino.message.composeapp.generated.resources.error
import com.dino.message.composeapp.generated.resources.ok
import com.dino.message.composeapp.generated.resources.successful
import com.dino.message.corefeature.domain.model.CustomMessageDialogData
import com.dino.message.corefeature.domain.model.CustomMessageDialogType
import com.dino.message.corefeature.presentation.icons.AppIcons
import com.dino.message.corefeature.presentation.resource.asComposeString
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import org.jetbrains.compose.resources.stringResource

@Composable
fun CustomMessageDialogContent(
    customMessageDialogData: CustomMessageDialogData,
    onDismiss: () -> Unit,
) {
    val header = when (customMessageDialogData.type) {
        CustomMessageDialogType.Error.type -> Res.string.error
        CustomMessageDialogType.Success.type -> Res.string.successful
        else -> Res.string.empty
    }

    val contextColor = when (customMessageDialogData.type) {
        CustomMessageDialogType.Error.type -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onBackground
    }

    Column(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                when (customMessageDialogData.type) {
                    CustomMessageDialogType.Error.type -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.background
                }
            )
            .padding(DPSmall)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = when (customMessageDialogData.type) {
                    CustomMessageDialogType.Error.type -> AppIcons.ErrorOutline
                    CustomMessageDialogType.Success.type -> AppIcons.CheckCircle
                    else -> AppIcons.CheckCircle
                },
                contentDescription = stringResource(Res.string.dialog_type_icon),
                tint = contextColor,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(DPMedium))

            Text(
                text = stringResource(header),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(end = DPLarge),
                color = contextColor
            )
        }

        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = DPExtraLarge)
        ) {
            items(customMessageDialogData.messages.size) { message ->
                Text(
                    text = customMessageDialogData.messages[message].title.asComposeString(),
                    color = contextColor,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = customMessageDialogData.messages[message].descriptions.asComposeString(),
                    color = contextColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(DPLarge))
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(end = DPExtraLarge)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008955))
                    ) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            }
        }
    }
}
