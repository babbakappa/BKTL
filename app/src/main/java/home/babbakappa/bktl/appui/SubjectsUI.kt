package home.babbakappa.bktl.appui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import home.babbakappa.bktl.database.TaskDao
import home.babbakappa.bktl.other.addSubjectsRaw
import home.babbakappa.bktl.other.importSubjectsFromTxt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsManagementDialog(
    dao: TaskDao,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Подписываемся на список предметов из БД
    val subjects by dao.observeAllSubjects().collectAsState(initial = emptyList())

    var importMode by remember { mutableStateOf<String?>(null) } // "manual", "file" или null
    var manualInput by remember { mutableStateOf("") }

    val txtPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                importSubjectsFromTxt(context, uri, dao)
                importMode = null
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 650.dp)
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Управление предметами", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                if (importMode == null) {
                    // Выбор способа добавления
                    Text("Как вы хотите добавить предметы?", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { importMode = "manual" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Вручную")
                        }
                        Button(
                            onClick = {
                                importMode = "file"
                                txtPickerLauncher.launch("text/plain")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Из файла (.txt)")
                        }
                    }
                } else if (importMode == "manual") {
                    // Форма ручного ввода
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = manualInput,
                            onValueChange = { manualInput = it },
                            label = { Text("Предметы (через запятую или Enter)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { importMode = null }) {
                                Text("Назад")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (manualInput.isNotBlank()) {
                                        scope.launch {
                                            addSubjectsRaw(manualInput, dao)
                                            manualInput = ""
                                            importMode = null
                                        }
                                    }
                                }
                            ) {
                                Text("Добавить")
                            }
                        }
                    }
                } else if (importMode == "file") {
                    // Ожидание/завершение выбора файла
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Файл выбран или операция отменена")
                        TextButton(onClick = { importMode = null }) {
                            Text("Назад")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Существующие предметы (${subjects.size}):", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // Список текущих предметов в БД с возможностью удаления
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(subjects, key = { it.id }) { subject ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(subject.name, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        scope.launch { dao.deleteSubject(subject) }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Удалить предмет"
                                    )
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
                    Button(
                        onClick = onDismiss
                    ) {
                        Text("Закрыть")
                    }
                    TextButton(
                        onClick = {
                            scope.launch { dao.deleteAllSubjects() }
                        }
                    ) {
                        Text("Очистить все")
                    }
                }
            }
        }
    }
}
