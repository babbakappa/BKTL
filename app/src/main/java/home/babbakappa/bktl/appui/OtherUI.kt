package home.babbakappa.bktl.appui

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import home.babbakappa.bktl.database.AppDatabase
import home.babbakappa.bktl.core.Task
import home.babbakappa.bktl.core.TaskList
import home.babbakappa.bktl.other.getDeadLineColor
import home.babbakappa.bktl.other.importTasksFromExcel
import kotlinx.coroutines.launch


//Сам объект задача (прямоугольник) в интерфейсе
@Composable
fun TaskItem(task: Task,
             isSelected: Boolean,
             onClick: () -> Unit,
             doTheButtons: Boolean,
             onEditClick: () -> Unit,
             onDeleteClick: () -> Unit) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    //Название предмета
                    Text(task.GetSubjectName(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    //Описание задачи
                    Text(task.GetDescription().replace("[NEWLINE]", "\n"), fontSize = 16.sp)
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
                        fontSize = 13.sp
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
                                onClick = onEditClick
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Редактировать"
                                )
                            }

                            IconButton(
                                onClick = onDeleteClick
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

//Для трех точек сверху
@Composable
fun ThreeDotsMenu(onMenuClick: (DialogType) -> Unit) {

    val context = LocalContext.current
    val url = "https://github.com/babbakappa/BKTL"
    var scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.get(context).taskDao() }
    val taskList = remember { TaskList(dao) }

    var expanded by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val success = importTasksFromExcel(context, uri, taskList)
                if (success) {
                    // Здесь можно показать Toast "Импорт успешно завершен"
                    Log.d("IMPORT", "Данные успешно импортированы")
                } else {
                    Log.e("IMPORT", "Ошибка при импорте данных")
                }
            }
        }
    }

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
            shape = RoundedCornerShape(16.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Удалить все") },
                onClick = {
                    expanded = false
                    onMenuClick(DialogType.DELETE_ALL)
                }
            )
            DropdownMenuItem(
                text = { Text("Архив задач") },
                onClick = {
                    expanded = false
                    onMenuClick(DialogType.ARCHIVE)
                }
            )
            DropdownMenuItem(
                text = { Text("Экспортировать (.xlsx)") },
                onClick = {
                    expanded = false
                    onMenuClick(DialogType.EXPORT)
                }
            )
            DropdownMenuItem(
                text = { Text("Импортировать (.xlsx)") },
                onClick = {
                    expanded = false
                    filePickerLauncher.launch("*/*")
                }
            )
            DropdownMenuItem(
                text = { Text("Управление предметами") },
                onClick = {
                    expanded = false
                    onMenuClick(DialogType.SUBJECTS)
                }
            )
            DropdownMenuItem(
                text = { Text("GitHub") },
                onClick = {
                    expanded = false
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }
            )
        }
    }
}