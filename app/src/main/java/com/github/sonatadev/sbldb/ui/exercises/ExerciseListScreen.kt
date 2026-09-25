package com.github.sonatadev.sbldb.ui.exercises

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.BackButton
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.MonoChip
import com.github.sonatadev.sbldb.ui.components.ScreenHeader
import com.github.sonatadev.sbldb.ui.components.SearchField
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme

@Composable
fun ExerciseListScreen(
    onOpenExercise: (Int) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: ExerciseListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    val picking = viewModel.pickTarget != null

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 16.dp)) {
        ScreenHeader(
            label = stringResource(if (picking) R.string.pick_label else R.string.library_label, state.exercises.size),
            title = stringResource(if (picking) R.string.add_exercise else R.string.all_exercises),
            navigation = onBack?.let { { BackButton(it) } },
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        SearchField(
            state.query,
            viewModel::setQuery,
            stringResource(R.string.search_exercises),
            Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                MonoChip(stringResource(R.string.all_groups), filled = state.selectedJoint == null, onClick = { viewModel.selectJoint(null) })
            }
            items(state.joints) { joint ->
                MonoChip(
                    joint,
                    filled = state.selectedJoint == joint,
                    onClick = { viewModel.selectJoint(if (state.selectedJoint == joint) null else joint) }
                )
            }
        }

        LazyColumn(contentPadding = PaddingValues(14.dp)) {
            item {
                Module(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                    Column {
                        state.exercises.forEachIndexed { i, item ->
                            val exercise = item.exercise
                            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (picking) viewModel.pick(exercise.exerciseId) { onBack?.invoke() }
                                        else onOpenExercise(exercise.exerciseId)
                                    }
                                    .padding(vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(exercise.name, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                                item.matchedAlias?.let {
                                    Text(stringResource(R.string.also_known_as_short, it), style = MaterialTheme.typography.bodySmall, color = colors.accent)
                                }
                                val equipment = exercise.attachment?.let { "${exercise.equipment} · $it" } ?: exercise.equipment
                                MonoCaption((item.primaryGroups + equipment).joinToString(" · "))
                            }
                        }
                    }
                }
            }
        }
    }
}
