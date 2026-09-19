//Файл ArchiveUI.kt
//Этот файл используется для отображения диалогового окна, в котором
//находится список удаленных задач. Используется в MainUI.kt

package home.babbakappa.bktl

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

//Функция диалога. Я бы ее лучше не трогал
@Composable
fun ArchiveDialog(
    //Задание параметров
    archive: List<Task>,
    onRestore: (Task) -> Unit,
    onDeleteForever: (Task) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {

    //Основной диалог
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(1f)
                .heightIn(max = 700.dp)
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = SetTopColor())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Архив задач", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SetTextColor())
                Spacer(modifier = Modifier.height(8.dp))
                if (archive.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Архив пуст", fontSize = 16.sp, color = SetTextColor())
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(archive, key = { it.GetId() }) { task ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(2.dp),
                                colors = CardDefaults.cardColors(containerColor = SetTaskColor())
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(task.GetSubjectName(), fontWeight = FontWeight.Bold, color = SetTextColor())
                                    Text(task.GetDescription().replace("[NEWLINE]", "\n"), fontSize = 12.sp, color = SetTextColor())
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Создано: ${task.GetCreationDate()}", fontSize = 10.sp, color = SetTextColor())
                                        Text("Дедлайн: ${task.GetExpireDate()}", fontSize = 10.sp, color = SetTextColor())
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onRestore(task) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(SetButtonColor())
                                        ) {
                                            Text("Восстановить", color = SetTextColor())
                                        }
                                        Button(
                                            onClick = { onDeleteForever(task) },
                                            colors = buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Удалить")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) { Text("Закрыть", color = SetTextColor()) }
                    Button(
                        onClick = onClearAll,
                        colors = buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Очистить архив")
                    }
                }
            }
        }
    }
}