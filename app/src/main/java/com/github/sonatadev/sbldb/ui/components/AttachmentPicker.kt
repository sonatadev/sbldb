package com.github.sonatadev.sbldb.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.data.Variants
import com.github.sonatadev.sbldb.data.entity.Exercise
import com.github.sonatadev.sbldb.data.repository.AttachmentOption
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType

private val ChipShape = RoundedCornerShape(percent = 50)

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
 * The attachment in use on a cable exercise, drawn and named in an outlined chip that opens the
 * list of attachments. Shows nothing for exercises that don't take one.
 */
@Composable
fun AttachmentChip(exercise: Exercise, loadOptions: suspend (Exercise) -> List<AttachmentOption>, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    if (!Variants.takesAttachments(exercise)) return
    val colors = SbldbTheme.colors
    var open by remember { mutableStateOf(false) }
    val current = exercise.attachment
    Row(
        modifier
            .clip(ChipShape)
            .border(1.dp, colors.edge, ChipShape)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.pick_attachment)) { open = true }
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        current?.let { AttachmentIcon(it, Modifier.size(width = 32.dp, height = 16.dp)) }
        Text(
            (current?.let { attachmentLabel(it) } ?: stringResource(R.string.pick_attachment)).uppercase() + "  ▾",
            style = SbldbType.label,
            color = colors.ink
        )
    }
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
    // Typing a new attachment: null while the list is shown
    var newName by remember { mutableStateOf<String?>(null) }
    fun pick(attachment: String) {
        onPick(attachment)
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.module,
        title = { Text(stringResource(if (newName == null) R.string.pick_attachment else R.string.new_attachment), color = colors.ink) },
        text = {
            val typing = newName
            if (typing != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextInput("", typing, { newName = it }, placeholder = stringResource(R.string.new_attachment_hint))
                    MonoCaption(stringResource(R.string.new_attachment_caption))
                }
            } else {
                Column {
                    MonoCaption(stringResource(R.string.attachment_hint, exercise.baseName))
                    LazyColumn(Modifier.heightIn(max = 440.dp)) {
                        items(options.orEmpty(), key = { it.attachment }) { option ->
                            val selected = option.attachment == current
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { pick(option.attachment) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                AttachmentIcon(
                                    option.attachment,
                                    Modifier.size(width = 44.dp, height = 22.dp),
                                    color = if (selected) colors.accent else colors.ink
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        attachmentLabel(option.attachment),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (selected) colors.accent else colors.ink
                                    )
                                    when {
                                        option.changesMovement -> MonoCaption(stringResource(R.string.attachment_changes_movement), color = colors.accent)
                                        option.isDefault -> MonoCaption(stringResource(R.string.attachment_default))
                                        option.isCustom -> MonoCaption(stringResource(R.string.attachment_yours))
                                    }
                                }
                                if (selected) Text("●", style = SbldbType.mono, color = colors.accent)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            // " · " separates an exercise from its attachment in names, so it can't be part of one
            val typing = newName?.replace("·", " ")?.replace(Regex("\\s+"), " ")?.trim()
            if (typing != null) {
                // A name that matches an existing attachment, whatever the case, picks that one
                val existing = options.orEmpty().firstOrNull { it.attachment.equals(typing, ignoreCase = true) || attachmentLabel(it.attachment).equals(typing, ignoreCase = true) }
                TextButton(onClick = { pick(existing?.attachment ?: typing) }, enabled = typing.isNotEmpty()) {
                    Text(stringResource(R.string.add), color = if (typing.isNotEmpty()) colors.accent else colors.dim)
                }
            } else {
                // Always in view, whatever the length of the list
                TextButton(onClick = { newName = "" }) {
                    Text("+ " + stringResource(R.string.new_attachment), color = colors.accent)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (newName != null) newName = null else onDismiss() }) {
                Text(stringResource(if (newName != null) R.string.back else R.string.cancel), color = colors.ink)
            }
        }
    )
}
