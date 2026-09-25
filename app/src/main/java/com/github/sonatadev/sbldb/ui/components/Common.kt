package com.github.sonatadev.sbldb.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme

@Composable
fun BackButton(onBack: () -> Unit) {
    RoundButton(glyph = "‹", description = stringResource(R.string.back), onClick = onBack)
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = SbldbTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.module,
        title = { Text(title, color = colors.ink) },
        text = { Text(message, color = colors.muted) },
        confirmButton = { TextButton(onClick = { onDismiss(); onConfirm() }) { Text(confirmLabel, color = colors.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = colors.ink) } }
    )
}
