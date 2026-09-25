package com.github.sonatadev.sbldb.ui.workout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.github.sonatadev.sbldb.domain.RestTimer
import com.github.sonatadev.sbldb.session.WorkoutService
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.data.entity.WorkoutExerciseWithSets
import com.github.sonatadev.sbldb.data.entity.WorkoutSet
import com.github.sonatadev.sbldb.domain.OneRepMax
import com.github.sonatadev.sbldb.domain.WeightUnit
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.CompactNumberField
import com.github.sonatadev.sbldb.ui.components.ConfirmDialog
import com.github.sonatadev.sbldb.ui.components.DotMatrix
import com.github.sonatadev.sbldb.ui.components.EffortMeter
import com.github.sonatadev.sbldb.ui.components.InfoLink
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.ModuleLabel
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.MonoChip
import com.github.sonatadev.sbldb.ui.components.PrimaryButton
import com.github.sonatadev.sbldb.ui.components.RoundButton
import com.github.sonatadev.sbldb.ui.components.SecondaryButton
import com.github.sonatadev.sbldb.ui.components.StatusDot
import com.github.sonatadev.sbldb.ui.formatClock
import com.github.sonatadev.sbldb.ui.formatSets
import com.github.sonatadev.sbldb.ui.formatTime
import com.github.sonatadev.sbldb.ui.theme.Geist
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType
import kotlinx.coroutines.delay

/** Operations on logged sets, shared by the workout in progress and by editing a past workout. */
interface SetActions {
    fun updateWeight(set: WorkoutSet, weightKg: Double?)
    fun updateReps(set: WorkoutSet, reps: Int?)
    fun updateRir(set: WorkoutSet, rir: Int?)
    fun toggleCompleted(set: WorkoutSet)
    fun toggleWarmup(set: WorkoutSet)
    fun deleteSet(set: WorkoutSet)
}

internal val SetColumn = 30.dp
internal val EffortColumn = 96.dp
internal val ActionColumn = 32.dp

@Composable
internal fun SetRow(
    set: WorkoutSet,
    label: Int?,
    isCurrent: Boolean,
    unit: WeightUnit,
    targetRir: Int?,
    actions: SetActions,
    /** Editing a past workout: every set keeps its fields editable and can be deleted. */
    alwaysEditable: Boolean = false
) {
    val showAsDone = set.isCompleted && !alwaysEditable
    val colors = SbldbTheme.colors
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (showAsDone) 44.dp else 54.dp)
                .then(if (showAsDone) Modifier.background(colors.accentTint, MaterialTheme.shapes.small) else Modifier),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label?.let { "%02d".format(it) } ?: stringResource(R.string.warmup_short),
                style = SbldbType.mono,
                color = when {
                    isCurrent -> colors.accent
                    set.isWarmup -> colors.muted
                    else -> colors.dim
                },
                modifier = Modifier
                    .width(SetColumn)
                    .clickable(onClickLabel = stringResource(R.string.toggle_warmup)) { actions.toggleWarmup(set) }
                    .padding(vertical = 12.dp)
            )
            if (showAsDone) {
                Text(
                    buildString {
                        if (set.weightKg != null) append("${unit.format(set.weightKg)} ${unit.label} ")
                        append("× ${set.reps ?: "–"}")
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    color = colors.ink,
                    modifier = Modifier.weight(1f)
                )
                Row(Modifier.width(EffortColumn), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EffortMeter(set.rir)
                    Text(set.rir?.let { "RIR $it" } ?: "RIR –", style = SbldbType.label, color = colors.muted)
                }
                Box(
                    Modifier
                        .size(ActionColumn)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClickLabel = stringResource(R.string.undo_set)) { actions.toggleCompleted(set) },
                    contentAlignment = Alignment.Center
                ) { StatusDot(colors.accent, size = 8.dp) }
            } else {
                EditableLoad(set, unit, actions, Modifier.weight(1f))
                EditableEffort(set, targetRir, actions, Modifier.width(EffortColumn))
                Box(
                    Modifier
                        .size(ActionColumn)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClickLabel = stringResource(R.string.delete_set)) { actions.deleteSet(set) },
                    contentAlignment = Alignment.Center
                ) { Text("×", style = SbldbType.monoLarge, color = colors.dim) }
            }
        }
    }
}

@Composable
private fun EditableLoad(set: WorkoutSet, unit: WeightUnit, actions: SetActions, modifier: Modifier) {
    val colors = SbldbTheme.colors
    // Local text state keyed on the set: DB re-emissions while typing must not reset the cursor
    var weight by rememberSaveable(set.setId, unit) { mutableStateOf(set.weightKg?.let(unit::format).orEmpty()) }
    var reps by rememberSaveable(set.setId) { mutableStateOf(set.reps?.toString().orEmpty()) }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CompactNumberField(
            value = weight,
            onValueChange = {
                weight = it
                actions.updateWeight(set, it.toDoubleOrNull()?.let(unit::toKg))
            },
            placeholder = unit.label,
            decimal = true,
            modifier = Modifier.weight(1.3f)
        )
        Text("×", color = colors.dim, fontFamily = Geist, fontSize = 16.sp)
        CompactNumberField(
            value = reps,
            onValueChange = {
                reps = it
                actions.updateReps(set, it.toIntOrNull())
            },
            placeholder = stringResource(R.string.col_reps).lowercase(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EditableEffort(set: WorkoutSet, targetRir: Int?, actions: SetActions, modifier: Modifier) {
    var rir by rememberSaveable(set.setId) { mutableStateOf(set.rir?.toString().orEmpty()) }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        EffortMeter(rir.toIntOrNull() ?: set.rir, outlineOnly = true)
        CompactNumberField(
            value = rir,
            onValueChange = {
                rir = it
                actions.updateRir(set, it.toIntOrNull())
            },
            placeholder = targetRir?.toString() ?: stringResource(R.string.col_rir),
            textStyle = SbldbType.mono.copy(fontSize = 14.sp),
            modifier = Modifier.weight(1f)
        )
    }
}
