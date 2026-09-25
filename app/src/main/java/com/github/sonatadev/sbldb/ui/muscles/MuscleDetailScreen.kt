package com.github.sonatadev.sbldb.ui.muscles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.data.entity.Muscle
import com.github.sonatadev.sbldb.data.entity.MuscleActionRow
import com.github.sonatadev.sbldb.data.entity.RatedExercise
import com.github.sonatadev.sbldb.data.entity.Role
import com.github.sonatadev.sbldb.data.repository.JointActionRepository
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.BackButton
import com.github.sonatadev.sbldb.ui.components.ExpandableRow
import com.github.sonatadev.sbldb.ui.components.ExpandableText
import com.github.sonatadev.sbldb.ui.components.SectionTabs
import com.github.sonatadev.sbldb.ui.components.firstSentence
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.RatingDots
import com.github.sonatadev.sbldb.ui.components.RoleChip
import com.github.sonatadev.sbldb.ui.components.ScreenHeader
import com.github.sonatadev.sbldb.ui.explained
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MuscleDetailUiState(
    val group: String = "",
    val entries: List<Muscle> = emptyList(),
    val actions: List<MuscleActionRow> = emptyList(),
    val exercises: List<RatedExercise> = emptyList()
) {
    val overview: Muscle? get() = entries.firstOrNull { it.muscleRegion == null }
    val regions: List<Muscle> get() = entries.filter { it.muscleRegion != null }
}

class MuscleDetailViewModel(savedStateHandle: SavedStateHandle, repository: JointActionRepository) : ViewModel() {
    private val group: String = checkNotNull(savedStateHandle["group"])

    val uiState: StateFlow<MuscleDetailUiState> = combine(
        repository.muscleGroup(group),
        repository.actionsForGroup(group),
        repository.bestExercisesForGroup(group)
    ) { entries, actions, exercises -> MuscleDetailUiState(group, entries, actions, exercises) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MuscleDetailUiState(group))
}

@Composable
fun MuscleDetailScreen(
    onBack: () -> Unit,
    onOpenAction: (Int) -> Unit,
    onOpenExercise: (Int) -> Unit,
    viewModel: MuscleDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val overviewInfo = state.overview?.let { explained(it.infoBasic, it.infoExpert) }
    val regionInfo = state.regions.map { explained(it.infoBasic, it.infoExpert) }
    val mainActions = state.actions.count { it.role == Role.PRIMARY }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenHeader(label = stringResource(R.string.muscle_label), title = state.group, navigation = { BackButton(onBack) })
        }
        overviewInfo?.let {
            item {
                Text(firstSentence(it), style = MaterialTheme.typography.bodyLarge, color = colors.muted, modifier = Modifier.padding(horizontal = 6.dp))
            }
        }
        item {
            SectionTabs(
                tabs = listOf(
                    stringResource(R.string.tab_overview),
                    stringResource(R.string.tab_actions_count, state.actions.size),
                    stringResource(R.string.tab_exercises_count, state.exercises.size)
                ),
                selected = tab,
                onSelect = { tab = it }
            )
        }
        when (tab) {
            0 -> {
                item {
                    Module(Modifier.fillMaxWidth()) {
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = colors.accent)) { append("$mainActions") }
                                append(" " + stringResource(R.string.summary_main_actions) + "  ·  ")
                                withStyle(SpanStyle(color = colors.accent)) { append("${state.regions.size}") }
                                append(" " + stringResource(R.string.summary_regions))
                            },
                            style = SbldbType.mono,
                            color = colors.muted
                        )
                    }
                }
                overviewInfo?.let { info ->
                    item {
                        Module(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                            ExpandableText(stringResource(R.string.section_about), info)
                        }
                    }
                }
                if (state.regions.isNotEmpty()) {
                    item {
                        Module(Modifier.fillMaxWidth(), label = stringResource(R.string.regions), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                            Column {
                                state.regions.forEachIndexed { i, region ->
                                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                                    ExpandableRow(title = region.muscleRegion.orEmpty()) {
                                        regionInfo[i]?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.muted) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> item {
                Module(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                    Column {
                        state.actions.forEachIndexed { i, action ->
                            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onOpenAction(action.jointActionId) }.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(Modifier.weight(1f)) {
                                    MonoCaption(action.joint)
                                    Text(action.name, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                                }
                                RoleChip(primary = action.role == Role.PRIMARY)
                                Text("›", style = SbldbType.monoLarge, color = colors.dim)
                            }
                        }
                    }
                }
            }
            else -> item {
                Module(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                    Column {
                        state.exercises.forEachIndexed { i, exercise ->
                            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onOpenExercise(exercise.exerciseId) }.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(exercise.name, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                                    MonoCaption(exercise.attachment?.let { "${exercise.equipment} · $it" } ?: exercise.equipment)
                                }
                                RatingDots(exercise.rating)
                            }
                        }
                    }
                }
            }
        }
    }
}
