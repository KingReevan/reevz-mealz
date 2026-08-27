package com.reevan.reevzmealz.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

private val GoodGreenLight = Color(0xFF2E7D32)
private val GoodGreenDark = Color(0xFF81C784)

/**
 * Green for "followed the plan".
 *
 * Material 3 has no success role, and the scheme is dynamic, so this one pair is chosen by theme
 * here rather than pulled from `colorScheme`. The matching "bad" colour is `colorScheme.error`,
 * which the scheme does provide.
 */
@Composable
fun goodColor(): Color = if (isSystemInDarkTheme()) GoodGreenDark else GoodGreenLight
