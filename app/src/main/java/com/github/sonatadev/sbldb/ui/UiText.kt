package com.github.sonatadev.sbldb.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** A message from a ViewModel: a string resource (translated) or raw text such as an error detail. */
sealed interface UiText {
    data class Res(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Raw(val text: String) : UiText
}

@Composable
fun UiText.resolve(): String = when (this) {
    is UiText.Res -> stringResource(id, *args.toTypedArray())
    is UiText.Raw -> text
}
