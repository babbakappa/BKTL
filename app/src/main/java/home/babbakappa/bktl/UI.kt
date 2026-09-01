//Файл UI.kt
//Один из главных файлов, который отвечает за интерфейс приложения

package home.babbakappa.bktl

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
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
import androidx.compose.ui.focus.focusModifier
import androidx.compose.material3.AlertDialog
import android.content.Intent
import android.widget.Button
import android.widget.TimePicker
import androidx.compose.animation.VectorConverter
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.composed
import home.babbakappa.bktl.SetTextColor
import org.apache.poi.ss.formula.functions.Column

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

//Сам объект задача (прямоугольник) в интерфейсе
@Composable
fun TaskItem(task: Task, isSelected: Boolean, onClick: () -> Unit) {

    //Цвет если выделено или не выделено
    val defcolor = SetTaskColor()
    var rescolor: Color
    val selcolor = SetSelectedTaskColor()
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
            //Название предмета
            Text(task.GetSubjectName(), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = SetTextColor())
            //Описание задачи
            Text(task.GetDescription().replace("[NEWLINE]", "\n"), fontSize = 16.sp, color = SetTextColor())

            //Один ряд с двумя датами
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                //Дата создания
                Text("Создано: ${task.GetCreationDate()}", fontSize = 13.sp, color = SetTextColor())

                //Определение цвета для даты дедлайна
                val color = getDeadLineColor(task.GetExpireDate())

                //Дата дедлайна
                Text("Дедлайн: ${task.GetExpireDate()}", fontSize = 13.sp, color = color)
            }
        }
    }
}

//Диалог добавления задачи
@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, String, String, String) -> Unit) {

    //Ебучка цвет рамок и текста полей ввода
    val need_colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SetTextColor(),       // Цвет рамки при фокусе
        unfocusedBorderColor = SetBGColor(),     // Цвет рамки без фокуса
        focusedLabelColor = SetTextColor(),        // Цвет текста подсказки при фокусе
        unfocusedLabelColor = SetBGColor(),       // Цвет текста подсказки без фокуса
        focusedTextColor = SetTextColor(),
        unfocusedTextColor = SetTextColor()
    )

    //Состояния на время диалога
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var creationDate by remember { mutableStateOf("") }
    var expireDate by remember { mutableStateOf("") }
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    //Нужно для того, чтобы обработать пустые поля при попытке ввода
    var showValidationError by remember { mutableStateOf(false) }

    //Показывает календарь для выбора даты
    fun showDatePicker(onDateSet: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val selectedDate = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                onDateSet(dateFormat.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    //Основной диалог
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(contentColor = SetTextColor(), containerColor = SetTopColor())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Добавление задачи", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Предмет", color = SetTextColor()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = need_colors
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание", color = SetTextColor()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = need_colors
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = creationDate,
                        onValueChange = { creationDate = it },
                        label = { Text("Дата создания", color = SetTextColor()) },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        colors = need_colors
                    )
                    Button(onClick = { showDatePicker { creationDate = it } }, colors = ButtonDefaults.buttonColors(containerColor = SetButtonColor())) {
                        Text("Выбрать", color = SetTextColor())
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = expireDate,
                        onValueChange = { expireDate = it },
                        label = { Text("Дедлайн", color = SetTextColor()) },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        colors = need_colors
                    )
                    Button(onClick = { showDatePicker { expireDate = it } }, colors = ButtonDefaults.buttonColors(containerColor = SetButtonColor())) {
                        Text("Выбрать", color = SetTextColor())
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Отмена", color = SetTextColor()) }
                    Button(
                        onClick = {
                            if (subject.isNotBlank() && description.isNotBlank() && creationDate.isNotBlank() && expireDate.isNotBlank()) {
                                onAdd(subject, description, creationDate, expireDate)
                            }

                            //Обработка пустого ввода
                            else {
                                showValidationError = true
                            }
                        },
                         colors = ButtonDefaults.buttonColors(containerColor = SetButtonColor())
                    ) { Text("Создать", color = SetTextColor()) }
                }
            }
        }
    }

    //Вывод месседжбокса при вводе пустых данных
    if (showValidationError) {
        AlertDialog(
            onDismissRequest = { showValidationError = false },
            title = { Text("Ошибка заполнения", color = SetTextColor()) },
            text = { Text("Пожалуйста, заполните все поля формы перед сохранением.", color = SetTextColor()) },
            confirmButton = {
                Button(onClick = { showValidationError = false }) {
                    Text("ОК", color = SetTextColor())
                }
            }
        )
    }
}


//Функция для редактирования задачи
@Composable
fun EditTaskDialog(onDismiss: () -> Unit, onAdd: (String, String, String, String) -> Unit, taskitself: Task) {

    //Состояния на время диалога
    var subject by remember { mutableStateOf(taskitself.GetSubjectName()) }
    var description by remember { mutableStateOf(taskitself.GetDescription().replace("[NEWLINE]", "\n")) }
    var creationDate by remember {mutableStateOf(taskitself.GetCreationDate())}
    var expireDate by remember { mutableStateOf(taskitself.GetExpireDate()) }
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    //Нужно для того, чтобы обработать пустые поля при попытке ввода
    var showValidationError by remember { mutableStateOf(false) }

    //Показывает календарь для выбора даты
    fun showDatePicker(onDateSet: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val selectedDate = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                onDateSet(dateFormat.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Показывает диалоговое окно для выбора времени
    fun showTimePicker(onTimeSet: (String) -> Unit) {

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val calendar = Calendar.getInstance()
        val is24HourFormat = android.text.format.DateFormat.is24HourFormat(context)

        TimePickerDialog(
            context,
            { _: TimePicker, hourOfDay: Int, minute: Int ->
                val selectedTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                onTimeSet(timeFormat.format(selectedTime.time))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            is24HourFormat
        ).show()
    }

    //Ебучка цвет рамок и текста полей ввода
    val need_colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SetTextColor(),       // Цвет рамки при фокусе
        unfocusedBorderColor = SetBGColor(),     // Цвет рамки без фокуса
        focusedLabelColor = SetTextColor(),        // Цвет текста подсказки при фокусе
        unfocusedLabelColor = SetBGColor(),       // Цвет текста подсказки без фокуса
        focusedTextColor = SetTextColor(),
        unfocusedTextColor = SetTextColor()
    )

    //Основной диалог
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = SetTopColor(), contentColor = SetTextColor())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Редактирование задачи", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SetTextColor())
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Предмет", color = SetTextColor()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = need_colors
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание", color = SetTextColor()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = need_colors
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = creationDate,
                        onValueChange = { creationDate = it },
                        label = { Text("Дата создания", color = SetTextColor()) },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        colors = need_colors
                    )
                    Button(onClick = { showDatePicker { creationDate = it } },
                        colors = ButtonDefaults.buttonColors(SetButtonColor())) {
                        Text("Выбрать", color = SetTextColor())
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = expireDate,
                        onValueChange = { expireDate = it },
                        label = { Text("Дата дедлайна", color = SetTextColor())  },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        colors = need_colors
                    )
                    Button(onClick = { showDatePicker{ expireDate = it } },
                        colors = ButtonDefaults.buttonColors(SetButtonColor())) {
                        Text("Выбрать", color = SetTextColor())
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Отмена", color = SetTextColor())  }
                    Button(
                        onClick = {
                            if (subject.isNotBlank() && creationDate.isNotBlank() && expireDate.isNotBlank() && description.isNotBlank()) {
                                onAdd(subject, description, creationDate, expireDate)
                            }

                            //Обработка пустого ввода
                            else {
                                showValidationError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(SetButtonColor())
                    ) { Text("Изменить", color = SetTextColor()) }
                }
            }
        }
    }
}


//Функция загрузки данных из файла БД
fun loadTasks(filePath: String): List<Task> = LoadArrayFromFile(filePath)