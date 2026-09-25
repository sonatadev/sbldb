package com.github.sonatadev.sbldb.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sonatadev.sbldb.ui.theme.Geist
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme

/** Underlined numeric input used on set rows; the underline turns accent while focused. */
@Composable
fun CompactNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    decimal: Boolean = false,
    textStyle: TextStyle = TextStyle(fontFamily = Geist, fontSize = 18.sp)
) {
    val colors = SbldbTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val style = textStyle.copy(color = colors.ink, textAlign = TextAlign.Center)
    BasicTextField(
        value = value,
        onValueChange = { raw ->
            val text = raw.replace(',', '.')
            val valid = if (decimal) text.matches(Regex("""\d{0,4}(\.\d{0,2})?""")) else text.matches(Regex("""\d{0,3}"""))
            if (valid) onValueChange(text)
        },
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(colors.accent),
        interactionSource = interaction,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        modifier = modifier,
        decorationBox = { inner ->
            Column {
                Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) Text(placeholder, style = style.copy(color = colors.dim))
                    inner()
                }
                Box(Modifier.fillMaxWidth().height(if (focused) 2.dp else 1.dp).background(if (focused) colors.accent else colors.edge))
            }
        }
    )
}
