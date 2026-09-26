package com.github.sonatadev.sbldb.ui.routines

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.RatingDots
import com.github.sonatadev.sbldb.ui.components.ScreenHeader
import com.github.sonatadev.sbldb.ui.components.SearchField
import com.github.sonatadev.sbldb.ui.components.firstSentence
import com.github.sonatadev.sbldb.ui.explained
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme

@Composable
fun MovementPickerScreen(
    onClose: () -> Unit,
    onOpenAction: (Int) -> Unit,
    viewModel: MovementPickerViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    val back = { if (!viewModel.back()) onClose() }
    BackHandler(onBack = back)
    if (!state.loaded) return
    val action = state.action
    val what = action?.let { explained(it.whatBasic, it.whatExpert) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            ScreenHeader(
                label = stringResource(if (action == null) R.string.movement_step_1 else R.string.movement_step_2),
                title = action?.let { "${it.joint} · ${it.name}" } ?: stringResource(R.string.movement_pick),
                navigation = { BackButton(back) }
            )
        }
        if (action == null) {
            item {
                SearchField(state.query, viewModel::setQuery, stringResource(R.string.search_actions), Modifier.padding(horizontal = 6.dp, vertical = 10.dp))
            }
            items(state.actions, key = { it.jointActionId }) { a ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.select(a.jointActionId) }
                        .padding(horizontal = 6.dp, vertical = 10.dp)
                ) {
                    MonoCaption(a.joint)
                    Text(a.name, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    a.primaryGroups?.let { MonoCaption(it.replace(",", " · "), color = colors.accent) }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
            }
        } else {
            item {
                Column(Modifier.padding(horizontal = 6.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    what?.let { Text(firstSentence(it), style = MaterialTheme.typography.bodyLarge, color = colors.muted) }
                    Text(
                        stringResource(R.string.movement_learn) + "  ›",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.accent,
                        modifier = Modifier.clickable { onOpenAction(action.jointActionId) }.padding(vertical = 4.dp)
                    )
                    MonoCaption(stringResource(R.string.movement_pick_exercise))
                }
            }
            items(state.exercises, key = { it.exerciseId }) { e ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.pick(e.exerciseId, onClose) }
                        .padding(horizontal = 6.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(e.name, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                        MonoCaption(e.attachment?.let { "${e.equipment} · $it" } ?: e.equipment)
                    }
                    RatingDots(e.rating)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
            }
        }
    }
}
