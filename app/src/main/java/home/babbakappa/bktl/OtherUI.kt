package home.babbakappa.bktl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


//Сам объект задача (прямоугольник) в интерфейсе
@Composable
fun TaskItem(task: Task,
             isSelected: Boolean,
             onClick: () -> Unit,
             doTheButtons: Boolean,
             onEditClick: () -> Unit,   // Добавили
             onDeleteClick: () -> Unit,
             defcolor: Color,
             selcolor: Color) {

    //Цвет если выделено или не выделено
    var rescolor: Color
    if (isSelected) {
        rescolor = selcolor
    }
    else {
        rescolor = defcolor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = rescolor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween ) {
                Column(modifier = Modifier.weight(1f)) {
                    //Название предмета
                    Text(task.GetSubjectName(), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = SetTextColor())
                    //Описание задачи
                    Text(task.GetDescription().replace("[NEWLINE]", "\n"), fontSize = 16.sp, color = SetTextColor())
                }

            }
            //Один ряд с двумя датами
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column() {

                    //Дата создания
                    Text(
                        "Создано: ${task.GetCreationDate()}",
                        fontSize = 13.sp,
                        color = SetTextColor()
                    )

                    //Определение цвета для даты дедлайна
                    val color = getDeadLineColor(task.GetExpireDate())

                    //Дата дедлайна
                    Text("Дедлайн: ${task.GetExpireDate()}", fontSize = 13.sp, color = color)
                }

                if (doTheButtons) {
                    Column() {
                        Row() {
                            IconButton(
                                onClick = onEditClick,
                                colors = IconButtonColors(containerColor = Color(0x00000000), disabledContainerColor = Color(0x00000000),
                                    contentColor = SetTextColor(), disabledContentColor = SetTextColor())
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Редактировать"
                                )
                            }

                            IconButton(
                                onClick = onDeleteClick,
                                colors = IconButtonColors(containerColor = Color(0x00000000), disabledContainerColor = Color(0x00000000),
                                    contentColor = SetTextColor(), disabledContentColor = SetTextColor())
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Удалить"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun whatSmileToPut(task: Task): String {
    when (task.GetSubjectName()) {
        "Иностранный язык" -> return "\uD83C\uDF10"
        "Алгоритмизация и программирование" -> return "\uD83D\uDDA5\uFE0F"
        "Вычислительная математика" -> return "\uD83D\uDD22"
        "Дискретная математика" -> return "\uD83E\uDDE9"
        "Математический анализ" -> return "♾\uFE0F"
        "Профессионально-прикладная физическая культура" -> return "\uD83C\uDFC3\u200D♂\uFE0F"
        "Теория информационных процессов и систем" -> return "\uD83D\uDCCA"
        "Технология программирования" -> return "\uD83D\uDEE0\uFE0F"
        "Физика" -> return "\uD83E\uDDF2"
        else -> return ""
    }

}




//Для трех точек сверху
@Composable
fun ThreeDotsMenu(onMenuClick: (DialogType) -> Unit) {

    val currentTextColor = SetTextColor()
    val currentBGColor = SetBGColor()
    val currentTaskColor = SetTaskColor()

    var expanded by remember { mutableStateOf(false) }

    Column {
        IconButton(
            onClick = { expanded = !expanded }
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Меню"
            )
        }

        // Выпадающий список
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = if (isSystemInDarkTheme()) currentBGColor else currentTaskColor
        ) {
            DropdownMenuItem(
                text = { Text("Удалить все", color = currentTextColor) },
                onClick = {
                    expanded = false
                    onMenuClick(DialogType.DELETE_ALL)
                }
            )
            DropdownMenuItem(
                text = { Text("Архив задач", color = currentTextColor) },
                onClick = {
                    expanded = false
                    onMenuClick(DialogType.ARCHIVE)
                }
            )
            DropdownMenuItem(
                text = { Text("Экспортировать", color = currentTextColor) },
                onClick = {
                    expanded = false
                    onMenuClick(DialogType.EXPORT)
                }
            )
        }
    }

}