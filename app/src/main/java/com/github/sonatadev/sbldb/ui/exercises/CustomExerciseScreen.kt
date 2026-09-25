package com.github.sonatadev.sbldb.ui.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.data.entity.JointActionSummary
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.BackButton
import com.github.sonatadev.sbldb.ui.components.ConfirmDialog
import com.github.sonatadev.sbldb.ui.components.InfoLink
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.MonoChip
import com.github.sonatadev.sbldb.ui.components.PrimaryButton
import com.github.sonatadev.sbldb.ui.components.RatingPicker
import com.github.sonatadev.sbldb.ui.components.RoleChip
import com.github.sonatadev.sbldb.ui.components.ScreenHeader
import com.github.sonatadev.sbldb.ui.components.SearchField
import com.github.sonatadev.sbldb.ui.components.SecondaryButton
import com.github.sonatadev.sbldb.ui.components.TextInput
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType
import com.github.sonatadev.sbldb.data.entity.Role as MuscleRole

private val EquipmentOptions = listOf("Barbell", "Dumbbell", "Cable", "Machine", "Bodyweight", "Band", "Other")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomExerciseScreen(
    onBack: () -> Unit,
    onSaved: (exerciseId: Int) -> Unit,
    onRemoved: () -> Unit,
    onOpenGlossary: (String) -> Unit,
    viewModel: CustomExerciseViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions by viewModel.actions.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    var pickAction by rememberSaveable { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }

    LaunchedEffect(state.finishedWith) {
        when (val id = state.finishedWith) {
            null -> Unit
            -1 -> onRemoved()
            else -> onSaved(id)
        }
    }
    if (!state.loaded) return
    val form = state.form
    val byId = actions.associateBy { it.jointActionId }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenHeader(
                label = stringResource(R.string.custom_label),
                title = stringResource(if (state.isNew) R.string.new_exercise else R.string.edit_exercise),
                navigation = { BackButton(onBack) }
            )
        }
        item {
            Module(Modifier.fillMaxWidth(), label = stringResource(R.string.module_basics)) {
                TextInput(stringResource(R.string.field_name), form.name, { v -> viewModel.edit { it.copy(name = v) } }, placeholder = "Incline Machine Press")
                MonoCaption(stringResource(R.string.field_equipment))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EquipmentOptions.forEach { option ->
                        val selected = option == form.equipment
                        MonoChip(option, filled = selected, onClick = { viewModel.edit { it.copy(equipment = option) } })
                    }
                }
                TextInput(stringResource(R.string.field_attachment), form.attachment, { v -> viewModel.edit { it.copy(attachment = v) } }, placeholder = "Rope, V-handle…")
                TextInput(stringResource(R.string.field_note), form.note, { v -> viewModel.edit { it.copy(note = v) } }, singleLine = false)
                TextInput(stringResource(R.string.field_aliases), form.aliases, { v -> viewModel.edit { it.copy(aliases = v) } }, placeholder = stringResource(R.string.aliases_hint))
            }
        }
        item {
            Module(
                Modifier.fillMaxWidth(),
                label = stringResource(R.string.module_actions_2),
                trailing = { InfoLink(stringResource(R.string.rating_short), onClick = { onOpenGlossary("Rating") }) }
            ) {
                if (form.ratings.isEmpty()) {
                    Text(stringResource(R.string.custom_actions_empty), style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                }
                Column {
                    form.ratings.entries.forEachIndexed { i, (actionId, rating) ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                        val action = byId[actionId]
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                MonoCaption(action?.joint.orEmpty())
                                Text(action?.name.orEmpty(), style = MaterialTheme.typography.titleMedium, color = colors.ink)
                            }
                            RatingPicker(rating, onSelect = { viewModel.setRating(actionId, it) })
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.remove)) { viewModel.removeAction(actionId) },
                                contentAlignment = Alignment.Center
                            ) { Text("×", style = SbldbType.monoLarge, color = colors.dim) }
                        }
                    }
                }
                Text(
                    "+ " + stringResource(R.string.add_joint_action).uppercase(),
                    style = SbldbType.mono,
                    color = colors.accent,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable(role = Role.Button) { pickAction = true }
                        .padding(vertical = 8.dp)
                )
            }
        }
        if (state.derived.isNotEmpty()) {
            item {
                Module(
                    Modifier.fillMaxWidth(),
                    label = stringResource(R.string.module_counts_toward),
                    trailing = { InfoLink(stringResource(R.string.roles), onClick = { onOpenGlossary("Prime mover") }) }
                ) {
                    state.derived.forEach { muscle ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(muscle.label, style = MaterialTheme.typography.bodyLarge, color = colors.ink, modifier = Modifier.weight(1f))
                            RoleChip(primary = muscle.role == MuscleRole.PRIMARY)
                        }
                    }
                    MonoCaption(stringResource(R.string.custom_derived_hint))
                }
            }
        }
        state.error?.let { error ->
            item { Text(error, style = SbldbType.mono, color = colors.accent, modifier = Modifier.padding(horizontal = 6.dp)) }
        }
        item {
            PrimaryButton(
                stringResource(R.string.save),
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (!state.isNew) {
            item {
                SecondaryButton(
                    stringResource(R.string.delete_exercise),
                    onClick = { confirmRemove = true },
                    color = colors.muted,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (pickAction) {
        ActionPickerDialog(
            actions = actions.filter { it.jointActionId !in form.ratings },
            onPick = { viewModel.setRating(it, 3); pickAction = false },
            onDismiss = { pickAction = false }
        )
    }
    if (confirmRemove) {
        ConfirmDialog(
            title = stringResource(R.string.delete_exercise),
            message = stringResource(R.string.delete_exercise_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = viewModel::remove,
            onDismiss = { confirmRemove = false }
        )
    }
}

@Composable
private fun ActionPickerDialog(actions: List<JointActionSummary>, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val colors = SbldbTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    val shown = actions.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true) || it.joint.contains(query, ignoreCase = true) ||
            it.primaryGroups.orEmpty().contains(query, ignoreCase = true)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.module,
        title = { Text(stringResource(R.string.add_joint_action), color = colors.ink) },
        text = {
            Column {
                SearchField(query, { query = it }, placeholder = stringResource(R.string.search_actions))
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(shown, key = { it.jointActionId }) { action ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(action.jointActionId) }
                                .padding(vertical = 10.dp)
                        ) {
                            MonoCaption(action.joint)
                            Text(action.name, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                            action.primaryGroups?.let { MonoCaption(it.replace(",", " · "), color = colors.accent) }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = colors.ink) } }
    )
}
