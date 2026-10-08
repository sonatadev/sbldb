package com.github.sonatadev.sbldb.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.data.Variants
import com.github.sonatadev.sbldb.data.entity.Exercise
import com.github.sonatadev.sbldb.data.repository.AttachmentOption
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType

/** The attachment's name in the app's language; attachments typed by the user stay as they are. */
@Composable
fun attachmentLabel(attachment: String): String = when (attachment) {
    "D-Handle" -> stringResource(R.string.att_d_handle)
    "Rope" -> stringResource(R.string.att_rope)
    "Straight Bar" -> stringResource(R.string.att_straight_bar)
    "EZ Bar" -> stringResource(R.string.att_ez_bar)
    "V-Bar" -> stringResource(R.string.att_v_bar)
    "Wide Bar" -> stringResource(R.string.att_wide_bar)
    "Neutral-Grip Wide Bar" -> stringResource(R.string.att_neutral_wide_bar)
    "Ankle Strap" -> stringResource(R.string.att_ankle_strap)
    "Forearm Strap" -> stringResource(R.string.att_forearm_strap)
    else -> attachment
}

/**
 * The attachment in use on a cable exercise, as a chip that opens the list of attachments.
 * Shows nothing for exercises that don't take one.
 */
@Composable
fun AttachmentChip(exercise: Exercise, loadOptions: suspend (Exercise) -> List<AttachmentOption>, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    if (!Variants.takesAttachments(exercise)) return
    var open by remember { mutableStateOf(false) }
    val current = exercise.attachment
    MonoChip(
        (current?.let { attachmentLabel(it) } ?: stringResource(R.string.pick_attachment)) + "  ▾",
        modifier = modifier,
        filled = true,
        onClick = { open = true }
    )
    if (open) AttachmentDialog(exercise, current, loadOptions, onPick = { if (it != current) onPick(it) }, onDismiss = { open = false })
}

@Composable
private fun AttachmentDialog(
    exercise: Exercise,
    current: String?,
    loadOptions: suspend (Exercise) -> List<AttachmentOption>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = SbldbTheme.colors
    val options by produceState<List<AttachmentOption>?>(null, exercise.exerciseId) { value = loadOptions(exercise) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.module,
        title = { Text(stringResource(R.string.pick_attachment), color = colors.ink) },
        text = {
            Column {
                MonoCaption(stringResource(R.string.attachment_hint, exercise.baseName))
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(options.orEmpty(), key = { it.attachment }) { option ->
                        val selected = option.attachment == current
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPick(option.attachment)
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    attachmentLabel(option.attachment),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selected) colors.accent else colors.ink
                                )
                                when {
                                    option.changesMovement -> MonoCaption(stringResource(R.string.attachment_changes_movement), color = colors.accent)
                                    option.isDefault -> MonoCaption(stringResource(R.string.attachment_default))
                                }
                            }
                            if (selected) Text("●", style = SbldbType.mono, color = colors.accent)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = colors.ink) } }
    )
}
