package com.github.sonatadev.sbldb.ui.glossary

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import com.github.sonatadev.sbldb.ui.theme.SbldbType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.data.entity.GlossaryTerm
import com.github.sonatadev.sbldb.data.repository.JointActionRepository
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.BackButton
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.ScreenHeader
import com.github.sonatadev.sbldb.ui.explained
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class GlossaryViewModel(savedStateHandle: SavedStateHandle, repository: JointActionRepository) : ViewModel() {
    /** Term to scroll to and highlight, when opened from a "?" link. */
    val focusTerm: String? = savedStateHandle.get<String>("term")?.takeIf { it.isNotBlank() }

    val terms: StateFlow<List<GlossaryTerm>> =
        repository.glossary.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun GlossaryScreen(onBack: () -> Unit, viewModel: GlossaryViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val terms by viewModel.terms.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    val listState = rememberLazyListState()
    val focusIndex = terms.indexOfFirst { it.term.equals(viewModel.focusTerm, ignoreCase = true) }

    val texts = terms.map { explained(it.basic, it.expert).orEmpty() }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenHeader(label = stringResource(R.string.glossary_label, terms.size), title = stringResource(R.string.glossary), navigation = { BackButton(onBack) })
        }
        item {
            Module(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                Column {
                    terms.forEachIndexed { index, term ->
                        if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                        GlossaryRow(term.term, texts[index], focused = index == focusIndex)
                    }
                }
            }
        }
    }
}

/** One term per line with a one-line preview; tap to read it all. The focused term starts open. */
@Composable
private fun GlossaryRow(term: String, text: String, focused: Boolean) {
    val colors = SbldbTheme.colors
    var expanded by rememberSaveable(term) { mutableStateOf(focused) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 12.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(term, style = MaterialTheme.typography.titleMedium, color = if (focused) colors.accent else colors.ink, modifier = Modifier.weight(1f))
            Text(if (expanded) "−" else "+", style = SbldbType.monoLarge, color = colors.accent)
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (expanded) colors.ink else colors.muted,
            maxLines = if (expanded) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
