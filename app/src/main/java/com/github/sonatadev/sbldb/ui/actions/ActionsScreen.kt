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
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.ScreenHeader
import com.github.sonatadev.sbldb.ui.components.SearchField
import com.github.sonatadev.sbldb.ui.components.SecondaryButton
import com.github.sonatadev.sbldb.ui.formatTime
import androidx.compose.ui.text.style.TextOverflow
import com.github.sonatadev.sbldb.ui.explained
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType

@Composable
fun ActionsScreen(
    onOpenAction: (Int) -> Unit,
    onOpenAllExercises: () -> Unit,
    onOpenGlossary: () -> Unit,
    viewModel: ActionsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    val actionCount = state.joints.sumOf { it.actions.size }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenHeader(label = stringResource(R.string.actions_label, actionCount), title = stringResource(R.string.tab_actions))
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    MonoCaption(
                        state.lastSync?.let { stringResource(R.string.library_synced, formatTime(it)) } ?: stringResource(R.string.library_not_synced)
                    )
                    state.syncError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.accent, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                SecondaryButton(
                    stringResource(if (state.syncing) R.string.library_checking else R.string.sync_short),
                    onClick = viewModel::sync,
                    color = colors.accent
                )
            }
        }
        item {
            SearchField(state.query, viewModel::setQuery, stringResource(R.string.search_actions), Modifier.padding(horizontal = 6.dp))
        }
        itemsIndexed(state.joints, key = { _, group -> group.joint }) { index, group ->
            Module(Modifier.fillMaxWidth(), label = "%02d · %s".format(index + 1, group.joint)) {
                Column {
                    group.actions.forEachIndexed { i, action ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenAction(action.jointActionId) }
                                .padding(vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(action.name, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                                action.primaryGroups?.let { MonoCaption(it.replace(",", " · ")) }
                                explained(action.whatBasic, action.whatExpert)?.let { what ->
                                    Text(
                                        what.substringBefore(". ").trimEnd('.') + ".",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.muted,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text("›", style = SbldbType.monoLarge, color = colors.dim)
                        }
                    }
                }
            }
        }
        item {
            Module(Modifier.fillMaxWidth(), label = stringResource(R.string.glossary_short), onClick = onOpenGlossary) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.glossary), style = MaterialTheme.typography.titleMedium, color = colors.ink)
                        MonoCaption(stringResource(R.string.glossary_hint))
                    }
                    Text("›", style = SbldbType.monoLarge, color = colors.dim)
                }
            }
        }
        item {
            Module(Modifier.fillMaxWidth(), label = stringResource(R.string.all_exercises_label), onClick = onOpenAllExercises) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.all_exercises), style = MaterialTheme.typography.titleMedium, color = colors.ink, modifier = Modifier.weight(1f))
                    Text("›", style = SbldbType.monoLarge, color = colors.dim)
                }
            }
        }
    }
}
