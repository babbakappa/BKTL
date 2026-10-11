/* MainUI.kt
* Самый главный файл для интерфейса, управляет всем остальным
* в приложении: остальные элементы интерфейса, репозиторий, БД,
* планирование и удаление алярмов, экспорты и импорты
*
* Потом в приложении будет добавлен ViewModel и такого хаоса
* не будет, хотя приложение и так быстро работает */

package home.babbakappa.bktl.appui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import android.util.Log
import android.widget.Button
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import home.babbakappa.bktl.database.AppDatabase
import home.babbakappa.bktl.core.Archive
import home.babbakappa.bktl.other.NotificationHelper
import home.babbakappa.bktl.core.Task
import home.babbakappa.bktl.core.TaskList
import home.babbakappa.bktl.other.exportTasksToCsv

enum class DialogType { ADD, DELETE_ALL, ARCHIVE, EXPORT, EDIT, SUBJECTS }
enum class SortType {NONE, BY_NAME, BY_DEADLINE}
enum class FilterType {NONE, BY_NAME}

//Главная функция, в которой работает интерфейс и используется ядро приложения
@Composable
@OptIn(ExperimentalMaterial3Api::class) //костыль для нормального запуска приложения из-за какой-то функции
fun TaskListApp() {



    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()

    val configuration = LocalConfiguration.current

    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)

            insetsController.isAppearanceLightStatusBars = !darkTheme
        }
    }

    val scope = rememberCoroutineScope()

    //Текущий контекст приложения
    val context = LocalContext.current

    var currentSortType by remember { mutableStateOf<SortType?>(SortType.NONE) }
    var currentFilterType by remember { mutableStateOf<FilterType?>(FilterType.NONE) }
    var searchQuery by remember { mutableStateOf("") }

    val dao = remember { AppDatabase.get(context).taskDao() }
    val taskList = remember { TaskList(dao) }
    val archive  = remember { Archive(dao) }

// Подписки на Flow из Room — вместо state.tasks/state.archive
    val tasks        by taskList.tasksFlow.collectAsState(initial = emptyList())
    val archiveTasks by archive.archiveFlow.collectAsState(initial = emptyList())

// selectedIndex оставляем как отдельный стейт — это единственное, что было нужно из UIState
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var dialog by remember { mutableStateOf<DialogType?>(null) }
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }

    var dropdownSortExpanded by remember { mutableStateOf(false) }
    var dSE_close by remember { mutableStateOf(false) }

    var searchActive by remember { mutableStateOf(false) }

    fun moveToArchive(task: Task) {
        scope.launch {
            taskList.DeleteTask(task) // Теперь это suspend
            // Взаимодействие с контекстом/уведомлениями безопасно оставляем на Main потоке
            NotificationHelper.cancelNotification(task, context)
            selectedTaskId = null
        }
    }

    fun moveAllToArchive() {
        scope.launch {
            for (task in tasks) {
                NotificationHelper.cancelNotification(task, context)
            }
            taskList.DeleteEverything()
        }
    }


    //Основной экран приложения, содержит все, что есть
    Scaffold(
        topBar = {
                val neededHeight = screenHeightDp * 0.125f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(neededHeight)
                        .statusBarsPadding(), // Высота шапки приложения
                    contentAlignment = Alignment.Center
                ) {
                    if (!searchActive) {
                        // 1. Стандартная шапка
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Список задач",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            ThreeDotsMenu(onMenuClick = { dialog = it })
                        }
                    } else {
                        // 2. Полностью симметричное поле ввода (без скрытых отступов M3)
                        TextField(
                            value = searchQuery,
                            onValueChange = { newText ->
                                searchQuery = newText
                            },
                            placeholder = { Text("Поиск по названию", fontSize = 16.sp) },
                            leadingIcon = {
                                IconButton(onClick = {
                                    taskList.updateQuery(sort = SortType.NONE, filter = FilterType.BY_NAME, extra = searchQuery)
                                })
                                {
                                    Icon(Icons.Default.Search, contentDescription = "Поиск")
                                }
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        taskList.updateQuery(sort = SortType.NONE, filter = FilterType.NONE)
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Очистить")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp), // Закругленные края как у SearchBar
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(horizontal = 16.dp, vertical = 4.dp) // Отступы слева и справа до краев экрана
                        )
                    }
                }
        },





        //Нижняя панель, которая содержит все кнопки действий
        bottomBar = {

            val neededHeight = screenHeightDp * 0.15f
            Box(modifier = Modifier.fillMaxWidth().height(neededHeight)) {


                Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center)  {


                    Row(modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically) {

                        Column() {

                            if (!dSE_close) {
                                IconButton(onClick = {
                                    dropdownSortExpanded = true
                                },
                                    enabled = !searchActive
                                ) {
                                    Icon(
                                        contentDescription = "Сортировка",
                                        imageVector = Icons.Default.Sort,
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }
                            else {
                                IconButton(onClick = {
                                    dSE_close = false
                                    taskList.updateQuery(sort = SortType.NONE, filter = FilterType.NONE)
                                }) {
                                    Icon(
                                        contentDescription = "Закрыть",
                                        imageVector = Icons.Default.Close,
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }

                            if (dropdownSortExpanded) {

                                DropdownMenu(
                                    expanded = dropdownSortExpanded,
                                    onDismissRequest = { dropdownSortExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.7f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("По названиям") },
                                        onClick = {
                                            taskList.updateQuery(sort = SortType.BY_NAME, filter = FilterType.NONE)
                                            dropdownSortExpanded = false
                                            dSE_close = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("По дедлайнам") },
                                        onClick = {
                                            taskList.updateQuery(sort = SortType.BY_DEADLINE, filter = FilterType.NONE)
                                            dropdownSortExpanded = false
                                            dSE_close = true
                                        }
                                    )
                                }

                            }

                        }

                        IconButton(
                            onClick = { dialog = DialogType.ADD},
                            enabled = searchActive == false && dSE_close == false
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Добавить",
                                modifier = Modifier.size(64.dp)
                            )
                        }

                        if (!searchActive) {

                            IconButton(onClick = {
                                searchActive = true
                            },
                                enabled = !dSE_close

                            ) {
                                Icon(
                                    contentDescription = "Поиск",
                                    imageVector = Icons.Default.Search,
                                    modifier = Modifier.size(64.dp)
                                )
                            }

                        }
                        else {
                            IconButton(onClick = {
                                taskList.updateQuery(sort = SortType.NONE, filter = FilterType.NONE)
                                searchActive = false
                            }) {
                                Icon(
                                    contentDescription = "Закрыть",
                                    imageVector = Icons.Default.Close,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }


                    }

                }


            }



        }
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
                    Text("Нет задач", fontSize = 20.sp)
                }
            }

            //Если есть задачи
            else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks, key = { task -> task.id }) { task ->
                        TaskItem(
                            task = task,
                            isSelected = selectedTaskId == task.id, // Быстрое сравнение Long без indexOf
                            onClick = {
                                selectedTaskId = if (selectedTaskId == task.id) null else task.id
                            },
                            doTheButtons = selectedTaskId == task.id,
                            onEditClick = {
                                selectedTaskId = task.id
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
            AddTaskDialog(onDismiss = { dialog = null }, onAdd = { subject, desc, cd, ed ->
                scope.launch {
                    // Ждем завершения вставки и получаем задачу с реальным ID из базы данных
                    val realTask = taskList.CreateAndAddNewTaskWithReturn(subject, desc, cd, ed)
                    // Планируем уведомление, зная точный ID задачи
                    NotificationHelper.scheduleNotification(realTask, context)
                }
                dialog = null
            })
        }

        //Если выбрарно удалить все задачи
        DialogType.DELETE_ALL -> {
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text("Удалить все задачи?") },
                text = { Text("Задачи будут перемещены в архив.") },
                confirmButton = {
                    Button(
                        onClick = {
                            moveAllToArchive()
                            dialog = null
                        }
                    ) { Text("Удалить всё") }
                },
                dismissButton = {
                    TextButton(onClick = { dialog = null }) {
                        Text("Отмена")
                    }
                }
            )
        }

        //Если нажал кнопку "Архив"
        DialogType.ARCHIVE -> {
            ArchiveDialog(
                archive = archiveTasks,
                onRestore = { task -> scope.launch { archive.RemoveFromArchive(task) } },
                onDeleteForever = { task -> scope.launch { archive.DeleteForever(task) } },
                onClearAll = { scope.launch { archive.DeleteEverything() } },
                onDismiss = { dialog = null }
            )
        }

        //Если выбран экспорт в эксель
        //Лучше не лезть сюда, работает и ладно
        DialogType.EXPORT -> {
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text("Экспорт отчёта") },
                text = { Text("Создать файл с текущими задачами?") },
                confirmButton = {Button(
                    onClick = {

                        scope.launch {
                            exportTasksToCsv(context, tasks)
                            dialog = null
                        }

                    }
                ) { Text("Экспортировать") }},
                dismissButton = {
                    TextButton(onClick = {dialog = null} ) {
                        Text("Отмена")
                    }
                }
            )

        }

        //Если выбрано редактирование задачи
        DialogType.EDIT -> {
            val currentId = selectedTaskId
            val task = tasks.find { it.id == currentId }
            if (task != null) {
                EditTaskDialog(
                    taskitself = task,
                    onDismiss = { dialog = null
                        selectedIndex = null
                        selectedTaskId = null
                    },
                    onAdd = { subject, desc, cd, ed ->
                        NotificationHelper.cancelNotification(task, context)   // главный поток
                        scope.launch {
                            taskList.EditTask(task, subject, desc, cd, ed)     // suspend, мутирует task
                            NotificationHelper.scheduleNotification(task, context)
                        }
                        dialog = null
                        selectedIndex = null
                        selectedTaskId = null
                    }
                )
            }
        }

        DialogType.SUBJECTS -> {
            SubjectsManagementDialog(
                dao = dao,
                onDismiss = { dialog = null }
            )
        }

        //Обработка Null
        null -> {}
    }

}