package com.github.sonatadev.sbldb.ui.components

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
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

/** Multi-line note editor; saving an empty text clears the note. */
@Composable
fun NoteDialog(title: String, initial: String, placeholder: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = SbldbTheme.colors
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.module,
        title = { Text(title, color = colors.ink) },
        text = { TextInput(label = "", value = text, onValueChange = { text = it }, placeholder = placeholder, singleLine = false) },
        confirmButton = { TextButton(onClick = { onSave(text); onDismiss() }) { Text(stringResource(R.string.save), color = colors.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = colors.ink) } }
    )
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
