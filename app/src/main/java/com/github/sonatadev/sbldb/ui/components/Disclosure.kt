package com.github.sonatadev.sbldb.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType

/** Mono tabs with an accent underline under the selected one. */
@Composable
fun SectionTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = SbldbTheme.colors
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            tabs.forEachIndexed { i, label ->
                val isSelected = i == selected
                Column(
                    modifier = Modifier
                        // The underline takes the width of the label, not of the whole row
                        .width(IntrinsicSize.Max)
                        .clip(MaterialTheme.shapes.small)
                        .selectable(selected = isSelected, role = Role.Tab) { onSelect(i) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(label.uppercase(), style = SbldbType.mono, color = if (isSelected) colors.accent else colors.muted)
                    Box(Modifier.height(2.dp).fillMaxWidth().background(if (isSelected) colors.accent else Color.Transparent))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.edge))
    }
}

/**
 * A titled explanation that shows only its first line until tapped, so pages stay scannable.
 * Starts expanded when [initiallyExpanded] is set.
 */
@Composable
fun ExpandableText(label: String, text: String, modifier: Modifier = Modifier, initiallyExpanded: Boolean = false) {
    val colors = SbldbTheme.colors
    var expanded by rememberSaveable(label) { mutableStateOf(initiallyExpanded) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClickLabel = label) { expanded = !expanded }
            .padding(vertical = 10.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ModuleLabel(label, Modifier.weight(1f), color = colors.muted)
            Text(if (expanded) "−" else "+", style = SbldbType.monoLarge, color = colors.accent)
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (expanded) colors.ink else colors.muted,
            maxLines = if (expanded) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** A compact row that reveals [details] when tapped. */
@Composable
fun ExpandableRow(
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    trailing: @Composable () -> Unit = {},
    details: @Composable () -> Unit
) {
    val colors = SbldbTheme.colors
    var expanded by rememberSaveable(title, caption) { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 11.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                caption?.let { MonoCaption(it) }
            }
            trailing()
            Text(if (expanded) "−" else "+", style = SbldbType.monoLarge, color = colors.accent)
        }
        if (expanded) details()
    }
}

/** First sentence of a longer text, for one-line subtitles. */
fun firstSentence(text: String): String = text.substringBefore(". ").trimEnd('.') + "."
