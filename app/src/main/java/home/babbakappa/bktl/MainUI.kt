//Файл MainUI.kt
//Один из главных файлов, который отвечает за интерфейс приложения

package home.babbakappa.bktl

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color

enum class DialogType { ADD, DELETE_ALL, ARCHIVE, EXPORT, EDIT }

//Главная функция, в которой работает интерфейс и используется ядро приложения
@Composable
@OptIn(ExperimentalMaterial3Api::class) //костыль для нормального запуска приложения из-за какой-то функции
fun TaskListApp() {
    //Текущий контекст приложения
    val context = LocalContext.current

    val dao = remember { AppDatabase.get(context).taskDao() }
    val taskList = remember { TaskList(dao) }
    val archive  = remember { Archive(dao) }

// Подписки на Flow из Room — вместо state.tasks/state.archive
    val tasks        by taskList.tasksFlow.collectAsState(initial = emptyList())
    val archiveTasks by archive.archiveFlow.collectAsState(initial = emptyList())

// selectedIndex оставляем как отдельный стейт — это единственное, что было нужно из UIState
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var dialog by remember { mutableStateOf<DialogType?>(null) }

    fun moveToArchive(task: Task) {
        taskList.DeleteTask(task) // = archive(id)
        selectedIndex = null
    }

    fun moveAllToArchive() {
        taskList.DeleteEverything()        // = archiveAll()
    }

    //Для трех точек сверху
    @Composable
    fun ThreeDotsMenu() {
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
                containerColor = SetBGColor()
            ) {
                DropdownMenuItem(
                    text = { Text("Удалить все", color = SetTextColor()) },
                    onClick = {
                        expanded = false
                        dialog = DialogType.DELETE_ALL
                    }
                )
                DropdownMenuItem(
                    text = { Text("Архив задач", color = SetTextColor()) },
                    onClick = {
                        expanded = false
                        dialog = DialogType.ARCHIVE
                    }
                )
                DropdownMenuItem(
                    text = { Text("Экспортировать", color = SetTextColor()) },
                    onClick = {
                        expanded = false
                        dialog = DialogType.EXPORT
                    }
                )
            }
        }


    }

    @Composable
    fun chosenToAdd() {
        AddTaskDialog(
            onDismiss = { dialog = null},
            onAdd = { subject, desc, cd, ed ->
                //Создается объект и добавляется в массив
                val newTask = taskList.CreateAndAddNewTaskWithReturn(subject, desc, cd, ed)
                NotificationHelper.scheduleNotification(newTask, context)

                dialog = null
            }
        )
    }

    @Composable
    fun chosenToDeleteAll() {
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Удалить все задачи?", color = SetTextColor()) },
            text = { Text("Задачи будут перемещены в архив.", color = SetTextColor()) },
            confirmButton = {
                Button(
                    onClick = {
                        moveAllToArchive()
                        dialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SetButtonColor())
                ) { Text("Удалить всё",  color = SetTextColor()) }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) {
                    Text("Отмена",  color = SetTextColor())
                }
            },
            containerColor = SetTopColor()
        )
    }


    @Composable
    fun chosenToEdit() {
        val idx = selectedIndex ?: return
        val task = tasks.getOrNull(idx) ?: return
        EditTaskDialog(
            onAdd = { subject, desc, cd, ed ->       // порядок: subject, description, creationDate, expireDate
                NotificationHelper.cancelNotification(task, context)
                taskList.EditTask(task, subject, desc, cd, ed)
                NotificationHelper.scheduleNotification(task, context)
                dialog = null
            },
            onDismiss = { dialog = null },
            taskitself = task
        )
    }

    @Composable
    fun chosenToExport() {
        val scope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { dialog = null},
            title = { Text("Экспорт отчёта",  color = SetTextColor()) },
            text = { Text("Создать файл с текущими задачами?", color = SetTextColor()) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val uri = exportTasksToExcel(context, tasks)
                            if (uri != null) {
                                // Открываем файл
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setData(uri)
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                context.startActivity(Intent.createChooser(intent, "Открыть отчёт"))
                            } else {
                                // Показать ошибку (Toast)
                            }
                            dialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(Color(0xFF107C41), SetTextColor())
                ) { Text("Экспортировать") }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) {
                    Text("Отмена", color = SetTextColor())
                }
            }
            ,containerColor = SetTopColor()
        )
    }

    @Composable
    fun chosenArchive() {
        ArchiveDialog(
            archive = archiveTasks,
            onRestore = { task -> archive.RemoveFromArchive(task) },
            onDeleteForever = { task -> archive.DeleteForever(task) },  // <-- новый метод
            onClearAll = { archive.DeleteEverything() },
            onDismiss = { dialog = null }
        )
    }

    //Основной экран приложения, содержит все, что есть
    Scaffold(
        //Шапка приложения
        topBar =
            { TopAppBar(
            title = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Список задач универа", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                    ThreeDotsMenu()
                }
                    },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SetTopColor(),
                titleContentColor = SetTextColor()
            )
        ) },

        //Нижняя панель, которая содержит все кнопки действий
        bottomBar = { Surface(modifier = Modifier.fillMaxWidth().height((128).dp), tonalElevation = 6.dp, color = SetBottomColor()) {

            //Колонна со всеми кнопками
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {

                //Верхний ряд с кнопками "Добавить" и "Удалить выбранное"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val fb = 240
                    //Кнопка добавления задачи
                    IconButton(
                        onClick = {dialog = DialogType.ADD},
                        colors = IconButtonColors(contentColor = SetTextColor(), containerColor = SetTopColor(),
                            disabledContentColor = SetTextColor(), disabledContainerColor = SetTopColor())
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Удалить",
                            modifier = Modifier.width(fb.dp).height(fb.dp)
                        )
                    }

                }

            }
        }
        },
        containerColor = SetBGColor()
    )

    //Добавление виджетов в основной экран (сами задачи в виде прямоугольников
    //с данными) или отображение что ничего нет
    { paddingValues ->

        //Бокс, в котором содержатся виджеты
        Box(modifier = Modifier.padding(paddingValues)) {

            //Если нет задач
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Задач пока нет", fontSize = 20.sp, color = SetTextColor())
                }
            }

            //Если есть задачи
            else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(tasks) { index, task ->
                        TaskItem(
                            task = task,
                            isSelected = selectedIndex == index,
                            onClick = {
                                selectedIndex = if (selectedIndex == index) null else index
                            },
                            doTheButtons = selectedIndex == index,
                            onEditClick = {
                                selectedIndex = index
                                dialog = DialogType.EDIT
                            },
                            onDeleteClick = {
                                moveToArchive(task)
                            }
                        )
                    }
                }
            }
        }
    }

    //Обработка диалогов
    when (dialog) {

        //Если выбрано добавить задачу
        DialogType.ADD -> {
            chosenToAdd()
        }

        //Если выбрарно удалить все задачи
        DialogType.DELETE_ALL -> {
            chosenToDeleteAll()
        }

        //Если нажал кнопку "Архив"
        DialogType.ARCHIVE -> {
            chosenArchive()
        }

        //Если выбран экспорт в эксель
        //Лучше не лезть сюда, работает и ладно
        DialogType.EXPORT -> {
            chosenToExport()
        }

        //Если выбрано редактирование задачи
        DialogType.EDIT -> {
            chosenToEdit()
        }

        //Обработка Null
        null -> {}
    }

}

//Функция загрузки данных из файла БД
fun loadTasks(filePath: String): List<Task> = LoadArrayFromFile(filePath)