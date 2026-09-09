//Файл MainUI.kt
//Один из главных файлов, который отвечает за интерфейс приложения

package home.babbakappa.bktl

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
import android.content.Intent
import android.widget.TimePicker
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text

enum class DialogType { ADD, DELETE_ALL, ARCHIVE, EXPORT, EDIT }

//Главная функция, в которой работает интерфейс и используется ядро приложения
@Composable
@OptIn(ExperimentalMaterial3Api::class) //костыль для нормального запуска приложения из-за какой-то функции
fun TaskListApp() {
    //Текущий контекст приложения
    val context = LocalContext.current
    //Данные из основной БД
    val dataFile = File(context.filesDir, DBFilePath).absolutePath
    val archiveFile = File(context.filesDir, ArchiveFilePath).absolutePath

    //Главные объекты, которые содержат все задачи
    val taskList = remember { TaskList() }
    val archive = remember { Archive() }

    //Состояние UI (теперь хранит только список задач и архив для отображения,
    //но они синхронизируются с taskList и archive)
    data class UIState(
        val tasks: List<Task> = emptyList(),
        val archive: List<Task> = emptyList(),
        val selectedIndex: Int? = null,
        val dialog: DialogType? = null
    )

    //Переменная состояния, то есть для tasks загружаются данные из
    //основной БД, для archive - из архивной БД
    var state by remember {
        mutableStateOf(
            UIState(
                tasks = loadTasks(dataFile),
                archive = loadTasks(archiveFile)
            )
        )
    }

    //При первом запуске заполняем главные объекты классов TaskList и
    //Archive загруженными данными
    LaunchedEffect(Unit) {
        //Загружаем данные из файлов в главные объекты
        val loadedTasks = loadTasks(dataFile)
        val loadedArchive = loadTasks(archiveFile)
        taskList.SetArray(loadedTasks)
        archive.SetArchive(loadedArchive)
        //Задаем переменной state новые данные
        state = state.copy(tasks = loadedTasks, archive = loadedArchive)
    }

    //Функция синхронизации переменной state с главными объектами.
    //Нужно вызывать при каждом изменении данных
    fun syncState() {
        state = state.copy(
            tasks = taskList.GetEntireTaskList().toList(),
            archive = archive.GetEntireArchive().toList(),
            selectedIndex = null // обычно сбрасываем выделение
        )
    }

    //Функция сохранения данных в основную БД
    fun saveTasks() {
        SaveArrayToFile(taskList.GetEntireTaskList(), dataFile)
    }

    //Функция сохранения данных в архивную БД
    fun saveArchive() {
        SaveArrayToFile(archive.GetEntireArchive(), archiveFile)
    }

    //Перемещение задачи в архив
    fun moveToArchive(task: Task) {
        //Удаляем из основного списка (по объекту)
        taskList.DeleteTask(task)
        //Добавляем в архив
        archive.AddTaskToArchive(task)
        //Синхронизируем интерфейс
        syncState()
        //Сохраняем
        saveTasks()
        saveArchive()
    }

    //Удаление всех задач (все добавляется в архив)
    fun moveAllToArchive() {
        //Берём все задачи из taskList
        val allTasks = taskList.GetEntireTaskList()
        //Добавляем их в архив
        for (t in allTasks) {
            archive.AddTaskToArchive(t)
        }
        //Очищаем основной список
        taskList.DeleteEverything()
        //Синхронизируем
        syncState()
        saveTasks()
        saveArchive()
    }

    //Основной экран приложения, содержит все, что есть
    Scaffold(
        //Шапка приложения
        topBar = { TopAppBar(
            title = { Text("Список задач универа", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SetTopColor(),
                titleContentColor = SetTextColor()
            )
        ) },

        //Нижняя панель, которая содержит все кнопки действий
        bottomBar = { Surface(modifier = Modifier.fillMaxWidth().height((128 + 24 + 64).dp), tonalElevation = 6.dp, color = SetBottomColor()) {

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

                    //Кнопка добавления задачи
                    Button(onClick = { state = state.copy(dialog = DialogType.ADD) }, colors = ButtonDefaults.buttonColors(contentColor = SetTextColor(), containerColor = SetButtonColor())) {
                        Text("Добавить")
                    }

                    //Кнопка добавления задачи
                    Button(onClick = { state = state.copy(dialog = DialogType.EDIT) }, colors = ButtonDefaults.buttonColors(contentColor = SetTextColor(), containerColor = SetButtonColor()), enabled = state.selectedIndex != null) {
                        Text("Редактировать")
                    }


                }

                //Ряд с кнопками
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    //Кнопка удаления выбранной задачи (перенос задачи в архив)
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = SetButtonColor(), contentColor = SetTextColor()),
                        onClick = {
                            state.selectedIndex?.let { idx ->
                                if (idx < state.tasks.size) {
                                    moveToArchive(state.tasks[idx])
                                }
                            }
                        },
                        enabled = state.selectedIndex != null
                    ) {
                        Text("Удалить выбранное")
                    }

                    //Кнопка удаления всех задач
                    Button(
                        onClick = { state = state.copy(dialog = DialogType.DELETE_ALL) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Удалить все")
                    }

                }

                //Нижний ряд с кнопкой "Экспорт в Excel"
                Row (modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically) {

                    //Кнопка открытия архива задач
                    Button(
                        onClick = { state = state.copy(dialog = DialogType.ARCHIVE) },
                        colors = ButtonDefaults.buttonColors(contentColor = SetTextColor(), containerColor = SetButtonColor())
                    ) {
                        Text("Архив")
                    }

                    Button(onClick = {state = state.copy(dialog = DialogType.EXPORT)},
                        colors = ButtonDefaults.buttonColors(contentColor = Color.White, containerColor = Color(0xFF008000))) {
                        Text("Экспорт в Excel", textAlign = TextAlign.Center)
                    }
                }

                //Костыль для внешнего вида, вот такой вот капризный Android
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {}
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
            if (state.tasks.isEmpty()) {
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
                    itemsIndexed(state.tasks) { index, task ->
                        TaskItem(
                            task = task,
                            isSelected = state.selectedIndex == index,
                            onClick = {
                                state = state.copy(
                                    selectedIndex = if (state.selectedIndex == index) null else index
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    //Обработка диалогов
    when (state.dialog) {

        //Если выбрано добавить задачу
        DialogType.ADD -> {
            AddTaskDialog(
                onDismiss = { state = state.copy(dialog = null) },
                onAdd = { subject, desc, cd, ed ->
                    //Создается объект и добавляется в массив
                    val newTask = taskList.CreateAndAddNewTaskWithReturn(subject, desc, cd, ed)
                    syncState()
                    saveTasks()
                    NotificationHelper.scheduleNotification(newTask, context)

                    state = state.copy(dialog = null)
                }
            )
        }

        //Если выбрарно удалить все задачи
        DialogType.DELETE_ALL -> {
            AlertDialog(
                onDismissRequest = { state = state.copy(dialog = null) },
                title = { Text("Удалить все задачи?", color = SetTextColor()) },
                text = { Text("Задачи будут перемещены в архив.", color = SetTextColor()) },
                confirmButton = {
                    Button(
                        onClick = {
                            moveAllToArchive()
                            state = state.copy(dialog = null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SetButtonColor())
                    ) { Text("Удалить всё",  color = SetTextColor()) }
                },
                dismissButton = {
                    TextButton(onClick = { state = state.copy(dialog = null) }) {
                        Text("Отмена",  color = SetTextColor())
                    }
                },
                containerColor = SetTopColor()
            )
        }

        //Если нажал кнопку "Архив"
        DialogType.ARCHIVE -> {
            ArchiveDialog(
                archive = state.archive, //отображаем актуальный архив
                onRestore = { task ->
                    // Восстанавливаем из архива
                    archive.RemoveFromArchive(task) //прямое удаление
                    taskList.AddTask(task) //добавление в основной лист обратно
                    syncState()
                    saveTasks()
                    saveArchive()
                },
                onDeleteForever = { task ->
                    //Удаляем навсегда из архива
                    archive.ArchiveArray.remove(task)
                    syncState()
                    saveArchive()
                },
                onClearAll = {
                    //Удаляем весь архив навсегда
                    archive.DeleteEverything()
                    syncState()
                    saveArchive()
                },
                //Отмена
                onDismiss = { state = state.copy(dialog = null) }
            )
        }

        //Если выбран экспорт в эксель
        //Лучше не лезть сюда, работает и ладно
        DialogType.EXPORT -> {
            val scope = rememberCoroutineScope()
            AlertDialog(
                onDismissRequest = { state = state.copy(dialog = null) },
                title = { Text("Экспорт отчёта",  color = SetTextColor()) },
                text = { Text("Создать файл с текущими задачами?", color = SetTextColor()) },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                val uri = exportTasksToExcel(context, state.tasks)
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
                                state = state.copy(dialog = null)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(SetButtonColor(), SetTextColor())
                    ) { Text("Экспортировать") }
                },
                dismissButton = {
                    TextButton(onClick = { state = state.copy(dialog = null) }) {
                        Text("Отмена", color = SetTextColor())
                    }
                }
                ,containerColor = SetTopColor()
            )
        }

        //Если выбрано редактирование задачи
        DialogType.EDIT -> {
            val index = state.selectedIndex!!
            val oldTask = taskList.GetTaskFromArrayByIndex(index)
            EditTaskDialog(
                onAdd = { subject, gr, desc, cd ->
                    NotificationHelper.cancelNotification(oldTask, context)
                    //Создается объект и добавляется в массив
                    taskList.EditTaskByIndex(state.selectedIndex!!, subject, gr, desc, cd)
                    syncState()
                    saveTasks()
                    val newTask = taskList.GetTaskFromArrayByIndex(index)
                    NotificationHelper.scheduleNotification(newTask, context)
                    state = state.copy(dialog = null)
                },
                onDismiss = { state = state.copy(dialog = null) },
                taskitself = taskList.GetTaskFromArrayByIndex(state.selectedIndex!!)
            )
        }

        //Обработка Null
        null -> {}
    }
}

//Функция загрузки данных из файла БД
fun loadTasks(filePath: String): List<Task> = LoadArrayFromFile(filePath)