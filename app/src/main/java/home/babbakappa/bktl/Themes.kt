/*Файл Themes.kt
*Этот файл содержит константы и функции для автоматического
*применения цветов виджетов в соответствии с установленной
*на телефоне теме (светлая или темная)*/

package home.babbakappa.bktl

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

//Для светлой темы
val LIGHT_TOP = Color(0xFFFFFFFF)
val LIGHT_BG = Color(0xFFBBBBBB)
val LIGHT_BOTTOM = Color(0xFFFFFFFF)
val LIGHT_BUTTON = Color(0xFF00AAFF)
val LIGHT_TASK = Color(0xFFEEEEEE)
val LIGHT_SELECTED_TASK = Color(0xFFAAAAAA)
val LIGHT_TEXT = Color.Black

//Для темной темы
val DARK_TOP = Color(0xFF000000)
val DARK_BG = Color(0xFF222222)
val DARK_BOTTOM = Color(0xFF000000)
val DARK_BUTTON = Color(0xFF333333)
val DARK_TASK = Color(0xFF555555)
val DARK_SELECTED_TASK = Color(0xFF151515)
val DARK_TEXT = Color.White

@Composable
fun SetTopColor(): Color {
    if (isSystemInDarkTheme()) {
        return DARK_TOP
    }
    else {
        return LIGHT_TOP
    }
}

@Composable
fun SetBGColor(): Color {
    if (isSystemInDarkTheme()) {
        return DARK_BG
    }
    else {
        return LIGHT_BG
    }
}

@Composable
fun SetBottomColor(): Color {
    if (isSystemInDarkTheme()) {
        return DARK_BOTTOM
    }
    else {
        return LIGHT_BOTTOM
    }
}

@Composable
fun SetButtonColor(): Color {
    if (isSystemInDarkTheme()) {
        return DARK_BUTTON
    }
    else {
        return LIGHT_BUTTON
    }
}

@Composable
fun SetTaskColor(): Color {
    if (isSystemInDarkTheme()) {
        return DARK_TASK
    }
    else {
        return LIGHT_TASK
    }
}

@Composable
fun SetSelectedTaskColor(): Color {
    if (isSystemInDarkTheme()) {
        return DARK_SELECTED_TASK
    }
    else {
        return LIGHT_SELECTED_TASK
    }
}

@Composable
fun SetTextColor(): Color {
    if (isSystemInDarkTheme()) {
        return DARK_TEXT
    }
    else {
        return LIGHT_TEXT
    }
}

@Composable
fun SetRedColor(): Color {
    if (isSystemInDarkTheme()) {
        return Color(0xFFFF4500)
    }
    else {
        return Color(0xFFFF0000)
    }
}

@Composable
fun SetPurpleColor(): Color {
    if (isSystemInDarkTheme()) {
        return Color(0xFFFF22FF)
    }
    else {
        return Color(0xFFFF00FF)
    }
}

@Composable
fun SetYellowColor(): Color {
    if (isSystemInDarkTheme()) {
        return Color(0xFFFFFF00)
    }
    else {
        return Color(0xFFEE9900)
    }
}
