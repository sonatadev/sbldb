package com.github.sonatadev.sbldb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType

private val Pill = RoundedCornerShape(percent = 50)

/** Main action: ink pill, optionally led by an accent dot. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentDot: Boolean = false
) {
    val colors = SbldbTheme.colors
    Row(
        modifier = modifier
            .heightIn(min = 54.dp)
            .clip(Pill)
            .background(if (enabled) colors.accent else colors.empty)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (accentDot) StatusDot(colors.onAccent, size = 8.dp)
        Text(text, color = if (enabled) colors.onAccent else colors.dim, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Outlined mono pill, e.g. "FINISH". */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = SbldbTheme.colors.ink
) {
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(Pill)
            .border(1.dp, SbldbTheme.colors.edge, Pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text.uppercase(), style = SbldbType.mono, color = color)
    }
}

/** Round icon-sized button with a text glyph, e.g. week arrows. */
@Composable
fun RoundButton(glyph: String, description: String, onClick: () -> Unit, enabled: Boolean = true) {
    val colors = SbldbTheme.colors
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (enabled) colors.module else Color.Transparent)
            .border(1.dp, colors.edge, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(glyph, style = SbldbType.monoLarge, color = if (enabled) colors.ink else colors.dim)
    }
}

@Composable
fun <T> SegmentedControl(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val colors = SbldbTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Pill)
            .background(colors.ground)
            .border(1.dp, colors.edge, Pill)
            .padding(3.dp)
            .selectableGroup()
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(Pill)
                    .background(if (isSelected) colors.accent else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(option) },
                contentAlignment = Alignment.Center
            ) {
                Text(label(option).uppercase(), style = SbldbType.mono, color = if (isSelected) colors.onAccent else colors.muted)
            }
        }
    }
}

/** Accent picker dot; the selected one gets a ring. */
@Composable
fun AccentSwatch(color: Color, selected: Boolean, description: String, onClick: () -> Unit) {
    val colors = SbldbTheme.colors
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(if (selected) Modifier.border(2.dp, colors.ink, CircleShape) else Modifier)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(if (selected) 26.dp else 30.dp).background(color, CircleShape))
    }
}

/** Filter or tag pill in mono caps. [filled] uses the accent. */
@Composable
fun MonoChip(text: String, modifier: Modifier = Modifier, filled: Boolean = false, onClick: (() -> Unit)? = null) {
    val colors = SbldbTheme.colors
    Box(
        modifier = modifier
            .clip(Pill)
            .then(
                if (filled) Modifier.background(colors.accent)
                else Modifier.border(1.dp, if (onClick != null) colors.edge else colors.accent, Pill)
            )
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = if (onClick != null) 9.dp else 4.dp)
    ) {
        Text(
            text.uppercase(),
            style = SbldbType.label,
            color = when {
                filled -> colors.onAccent
                onClick != null -> colors.ink
                else -> colors.accent
            }
        )
    }
}

/** Screen header: small mono label above a large title, with optional actions on the right. */
@Composable
fun ScreenHeader(
    label: String,
    title: String,
    modifier: Modifier = Modifier,
    titleHero: Boolean = false,
    navigation: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = SbldbTheme.colors
    Row(modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.Bottom) {
        if (navigation != null) {
            Box(Modifier.padding(end = 8.dp, bottom = 2.dp)) { navigation() }
        }
        androidx.compose.foundation.layout.Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ModuleLabel(label, color = colors.muted)
            Box(Modifier.size(width = 24.dp, height = 3.dp).clip(Pill).background(colors.accent))
            Text(
                title,
                style = if (titleHero) SbldbType.hero(56) else androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                color = if (titleHero) colors.accent else colors.ink,
                maxLines = 2
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Underlined search input with a mono glyph. */
@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val colors = SbldbTheme.colors
    androidx.compose.foundation.layout.Column(modifier) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⌕  ", style = SbldbType.monoLarge, color = colors.dim)
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(placeholder, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, color = colors.dim)
                        inner()
                    }
                }
            }
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.edge))
    }
}

/** Role chip for a muscle in a joint action; wording follows the explanation level. */
@Composable
fun RoleChip(primary: Boolean) {
    val expert = com.github.sonatadev.sbldb.ui.isExpert()
    val text = androidx.compose.ui.res.stringResource(
        when {
            primary && expert -> com.github.sonatadev.sbldb.R.string.role_prime_mover
            primary -> com.github.sonatadev.sbldb.R.string.role_main
            expert -> com.github.sonatadev.sbldb.R.string.role_synergist
            else -> com.github.sonatadev.sbldb.R.string.role_helper
        }
    )
    MonoChip(text, filled = primary)
}

/** Small "?" link that opens a glossary term. */
@Composable
fun InfoLink(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = SbldbTheme.colors
    Row(
        modifier = modifier
            .clip(Pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label.uppercase(), style = SbldbType.label, color = colors.muted)
        Box(Modifier.size(16.dp).border(1.dp, colors.accent, CircleShape), contentAlignment = Alignment.Center) {
            Text("?", style = SbldbType.label, color = colors.accent)
        }
    }
}
