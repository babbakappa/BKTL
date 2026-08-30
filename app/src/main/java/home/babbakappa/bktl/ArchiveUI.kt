//Файл ArchiveUI.kt
//Этот файл используется для отображения диалогового окна, в котором
//находится список удаленных задач. Используется в UI.kt

package home.babbakappa.bktl

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Архив задач", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                if (archive.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Архив пуст", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(archive) { task ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(task.GetSubjectName(), fontWeight = FontWeight.Bold)
                                    Text(task.GetDescription().replace("[NEWLINE]", "\n"), fontSize = 12.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Создано: ${task.GetCreationDate()}", fontSize = 10.sp)
                                        Text("Дедлайн: ${task.GetExpireDate()}", fontSize = 10.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onRestore(task) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Восстановить")
                                        }
                                        Button(
                                            onClick = { onDeleteForever(task) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
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
                    TextButton(onClick = onDismiss) { Text("Закрыть") }
                    Button(
                        onClick = onClearAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Очистить архив")
                    }
                }
            }
        }
    }
}