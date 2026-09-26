package com.github.sonatadev.sbldb.ui.actions

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
import androidx.compose.runtime.remember
import com.github.sonatadev.sbldb.domain.ActionAnimation
import com.github.sonatadev.sbldb.domain.Dof
import com.github.sonatadev.sbldb.domain.FigureView
import com.github.sonatadev.sbldb.ui.components.JointFigure
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.data.entity.RatedExercise
import com.github.sonatadev.sbldb.data.entity.Role
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.BackButton
import com.github.sonatadev.sbldb.ui.components.ExpandableRow
import com.github.sonatadev.sbldb.ui.components.ExpandableText
import com.github.sonatadev.sbldb.ui.components.InfoLink
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.RatingDots
import com.github.sonatadev.sbldb.ui.components.RoleChip
import com.github.sonatadev.sbldb.ui.components.ScreenHeader
import com.github.sonatadev.sbldb.ui.components.SectionTabs
import com.github.sonatadev.sbldb.ui.components.firstSentence
import com.github.sonatadev.sbldb.ui.explained
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType

private const val TAB_OVERVIEW = 0
private const val TAB_MUSCLES = 1
private const val TAB_EXERCISES = 2

@Composable
fun ActionDetailScreen(
    onBack: () -> Unit,
    onOpenExercise: (Int) -> Unit,
    onOpenMuscle: (String) -> Unit,
    onOpenGlossary: (String) -> Unit,
    viewModel: ActionDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    val action = state.action ?: return
    var tab by rememberSaveable { mutableIntStateOf(TAB_OVERVIEW) }

    val what = explained(action.whatBasic, action.whatExpert) ?: action.description
    val why = explained(action.whyBasic, action.whyExpert)
    val feel = explained(action.feelBasic, action.feelExpert)
    val mainCount = state.muscles.count { it.role == Role.PRIMARY }
    val helperCount = state.muscles.size - mainCount
    // Muscle notes are resolved here because LazyColumn content is not composable
    val muscleNotes = state.muscles.map { explained(it.noteBasic, it.noteExpert) }
    val animation = remember(action.animation) { ActionAnimation.decode(action.animation) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenHeader(label = action.joint, title = action.name, navigation = { BackButton(onBack) })
        }
        item {
            Text(firstSentence(what), style = MaterialTheme.typography.bodyLarge, color = colors.muted, modifier = Modifier.padding(horizontal = 6.dp))
        }
        item {
            SectionTabs(
                tabs = listOf(
                    stringResource(R.string.tab_overview),
                    stringResource(R.string.tab_muscles_count, state.muscles.size),
                    stringResource(R.string.tab_exercises_count, state.exercises.size)
                ),
                selected = tab,
                onSelect = { tab = it }
            )
        }

        when (tab) {
            TAB_OVERVIEW -> {
                animation?.let { anim ->
                    item {
                        Module(Modifier.fillMaxWidth(), label = stringResource(R.string.module_movement), trailing = {
                            MonoCaption(stringResource(if (anim.dof == Dof.FOREARM_ROT) R.string.view_top else viewLabel(anim.view)))
                        }) {
                            JointFigure(anim, "${action.joint} ${action.name}", Modifier.fillMaxWidth().height(220.dp))
                            MonoCaption(
                                "${anim.from.toInt()}${unitMark(anim)} → ${anim.to.toInt()}${unitMark(anim)}",
                                color = colors.accent
                            )
                        }
                    }
                }
                item {
                    Module(Modifier.fillMaxWidth()) {
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = colors.accent)) { append("$mainCount") }
                                append(" " + stringResource(R.string.summary_main) + "  ·  ")
                                withStyle(SpanStyle(color = colors.accent)) { append("$helperCount") }
                                append(" " + pluralStringResource(R.plurals.summary_helpers, helperCount) + "  ·  ")
                                withStyle(SpanStyle(color = colors.accent)) { append("${state.exercises.size}") }
                                append(" " + stringResource(R.string.summary_exercises))
                            },
                            style = SbldbType.mono,
                            color = colors.muted
                        )
                    }
                }
                item {
                    Module(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                        Column {
                            ExpandableText(stringResource(R.string.section_what_it_is), what)
                            why?.let { Divider(); ExpandableText(stringResource(R.string.section_why_it_matters), it) }
                            feel?.let { Divider(); ExpandableText(stringResource(R.string.section_how_to_feel_it), it) }
                        }
                    }
                }
                item {
                    Module(
                        Modifier.fillMaxWidth(),
                        label = stringResource(R.string.top_picks),
                        trailing = { InfoLink(stringResource(R.string.rating_short), onClick = { onOpenGlossary("Rating") }) }
                    ) {
                        Column {
                            state.exercises.take(3).forEachIndexed { i, exercise ->
                                if (i > 0) Divider()
                                ExerciseRow(exercise, onClick = { onOpenExercise(exercise.exerciseId) })
                            }
                        }
                        if (state.exercises.size > 3) {
                            Text(
                                stringResource(R.string.see_all_exercises, state.exercises.size) + "  ›",
                                style = SbldbType.mono,
                                color = colors.accent,
                                modifier = Modifier.clickable { tab = TAB_EXERCISES }.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            TAB_MUSCLES -> {
                item {
                    Module(
                        Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        trailing = { InfoLink(stringResource(R.string.roles), onClick = { onOpenGlossary("Prime mover") }) }
                    ) {
                        Column {
                            state.muscles.forEachIndexed { i, muscle ->
                                if (i > 0) Divider()
                                ExpandableRow(
                                    title = muscle.muscleRegion ?: muscle.muscleGroup,
                                    caption = muscle.muscleGroup.takeIf { muscle.muscleRegion != null },
                                    trailing = { RoleChip(primary = muscle.role == Role.PRIMARY) }
                                ) {
                                    muscleNotes[i]?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.muted) }
                                    Text(
                                        stringResource(R.string.about_muscle, muscle.muscleGroup) + "  ›",
                                        style = SbldbType.mono,
                                        color = colors.accent,
                                        modifier = Modifier.clickable { onOpenMuscle(muscle.muscleGroup) }.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                item { MonoCaption(stringResource(R.string.tap_to_expand), Modifier.padding(horizontal = 6.dp)) }
            }

            TAB_EXERCISES -> {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        MonoCaption(stringResource(R.string.sorted_by_rating), Modifier.weight(1f))
                        InfoLink(stringResource(R.string.rating_short), onClick = { onOpenGlossary("Rating") })
                    }
                }
                item {
                    Module(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                        Column {
                            state.exercises.forEachIndexed { i, exercise ->
                                if (i > 0) Divider()
                                ExerciseRow(exercise, onClick = { onOpenExercise(exercise.exerciseId) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: RatedExercise, onClick: () -> Unit) {
    val colors = SbldbTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(exercise.name, style = MaterialTheme.typography.titleMedium, color = colors.ink)
            MonoCaption(exercise.attachment?.let { "${exercise.equipment} · $it" } ?: exercise.equipment)
        }
        RatingDots(exercise.rating)
    }
}

private fun viewLabel(view: FigureView): Int = when (view) {
    FigureView.SIDE -> R.string.view_side
    FigureView.FRONT -> R.string.view_front
    FigureView.TOP -> R.string.view_top
}

/** Scapula moves are shifts, not angles, so they get no degree sign. */
private fun unitMark(animation: ActionAnimation): String =
    if (animation.dof == Dof.SCAP_ELEV || animation.dof == Dof.SCAP_PROTRACT) "" else "°"

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(SbldbTheme.colors.line))
}
