package com.github.sonatadev.sbldb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType

val ModuleShape = RoundedCornerShape(18.dp)

/** Instrument-panel module: a bordered panel with an optional mono label such as "03 · NOW". */
@Composable
fun Module(
    modifier: Modifier = Modifier,
    label: String? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = SbldbTheme.colors
    Column(
        modifier = modifier
            .clip(ModuleShape)
            .background(colors.module)
            .border(1.dp, colors.edge, ModuleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (label != null || trailing != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (label != null) ModuleLabel(label, Modifier.weight(1f))
                trailing?.invoke(this)
            }
        }
        content()
    }
}

private val numberedLabel = Regex("""^(#?\d+)( · .*)?$""")

/** Mono caps label; a leading number such as "03" in "03 · NOW" is drawn in the accent. */
@Composable
fun ModuleLabel(text: String, modifier: Modifier = Modifier, color: Color = SbldbTheme.colors.dim) {
    val upper = text.uppercase()
    val match = numberedLabel.matchEntire(upper)
    if (match == null) {
        Text(upper, style = SbldbType.label, color = color, modifier = modifier)
    } else {
        val accent = SbldbTheme.colors.accent
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = accent)) { append(match.groupValues[1]) }
                withStyle(SpanStyle(color = color)) { append(match.groupValues[2]) }
            },
            style = SbldbType.label,
            modifier = modifier
        )
    }
}

/** A small mono caption, e.g. "LAST 75 × 6". */
@Composable
fun MonoCaption(text: String, modifier: Modifier = Modifier, color: Color = SbldbTheme.colors.muted) {
    Text(text.uppercase(), style = SbldbType.mono.copy(fontSize = SbldbType.label.fontSize), color = color, modifier = modifier)
}

/** Hairline-separated row inside a module. */
@Composable
fun ModuleRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().background(SbldbTheme.colors.line).padding(top = 1.dp)) {}
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}
