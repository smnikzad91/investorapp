package ir.devtrader.investor.ui.update

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import ir.devtrader.investor.R
import ir.devtrader.investor.data.remote.dto.CheckUpdateResponse

@Composable
private fun updateMessage(info: CheckUpdateResponse, mandatory: Boolean): String {
    val lead = if (mandatory) {
        stringResource(R.string.update_required_message_format, info.latestVersionName ?: "")
    } else {
        stringResource(R.string.update_available_message_format, info.latestVersionName ?: "")
    }
    val notes = info.releaseNotes?.takeIf { it.isNotBlank() }
        ?.let { stringResource(R.string.update_whats_new_format, it) } ?: ""
    return lead + notes
}

@Composable
private fun confirmLabel(downloading: Boolean, failed: Boolean): String = when {
    downloading -> stringResource(R.string.update_button_downloading)
    failed -> stringResource(R.string.update_button_retry)
    else -> stringResource(R.string.update_button_now)
}

/** Non-cancelable: blocks both outside-tap dismissal and the hardware/gesture back action. */
@Composable
fun MandatoryUpdateDialog(state: UpdateUiState.Immediate, onUpdateNow: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.update_required_title)) },
        text = { Text(updateMessage(state.info, mandatory = true)) },
        confirmButton = {
            TextButton(onClick = onUpdateNow, enabled = !state.downloading) {
                Text(confirmLabel(state.downloading, state.failed))
            }
        },
    )
}

/** Dismissible: "Update Now", "Skip / Later", or tapping outside all let the user into the app. */
@Composable
fun OptionalUpdateDialog(state: UpdateUiState.Flexible, onUpdateNow: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.update_available_title)) },
        text = { Text(updateMessage(state.info, mandatory = false)) },
        confirmButton = {
            TextButton(onClick = onUpdateNow, enabled = !state.downloading) {
                Text(confirmLabel(state.downloading, state.failed))
            }
        },
        dismissButton = {
            TextButton(onClick = onLater, enabled = !state.downloading) { Text(stringResource(R.string.update_button_skip_later)) }
        },
    )
}
