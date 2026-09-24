//Файл MainUI.kt
//Один из главных файлов, который отвечает за интерфейс приложения

package home.babbakappa.bktl

import android.app.Activity
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
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class DialogType { ADD, DELETE_ALL, ARCHIVE, EXPORT, EDIT }

//Главная функция, в которой работает интерфейс и используется ядро приложения
@Composable
@OptIn(ExperimentalMaterial3Api::class) //костыль для нормального запуска приложения из-за какой-то функции
fun TaskListApp() {

    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)

            insetsController.isAppearanceLightStatusBars = !darkTheme
        }
    }

    val currentTextColor = SetTextColor()
    val currentTopColor = SetTopColor()
    val currentBottomColor = SetBottomColor()
    val currentTaskColor = SetTaskColor()
    val currentSelectedTaskColor = SetSelectedTaskColor()
    val currentButtonColor = SetButtonColor()

    val scope = rememberCoroutineScope()

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
        taskList.DeleteTask(task)
        NotificationHelper.cancelNotification(task, context)
        selectedIndex = null
    }

    fun moveAllToArchive() {
        scope.launch(Dispatchers.IO) {
            for (task in tasks) {
                NotificationHelper.cancelNotification(task, context)
            }
            taskList.DeleteEverything()
        }
    }


    //Основной экран приложения, содержит все, что есть
    Scaffold(
        //Шапка приложения
        topBar =
            { TopAppBar(
            title = {
                Row(modifier = Modifier.fillMaxWidth().statusBarsPadding(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Список задач универа", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                    ThreeDotsMenu(onMenuClick = { dialog = it })
                }
                    },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = currentTopColor,
                titleContentColor = currentTextColor
            )
        ) },

        //Нижняя панель, которая содержит все кнопки действий
        bottomBar = { Surface(modifier = Modifier.fillMaxWidth().height((128).dp), tonalElevation = 6.dp, color = currentBottomColor) {

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
                        onClick = {
                            Log.d("PERF", "click t=${System.currentTimeMillis()}")
                            dialog = DialogType.ADD},
                        colors = IconButtonColors(contentColor = currentTextColor, containerColor = currentButtonColor,
                            disabledContentColor = currentTextColor, disabledContainerColor = currentTopColor)
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
                    Text("Задач пока нет", fontSize = 20.sp, color = currentTextColor)
                }
            }

            //Если есть задачи
            else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(tasks, key = { _, task -> task.GetId() }) { index, task ->
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
                            },
                            currentTaskColor,
                            currentSelectedTaskColor
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
            AddTaskDialog(
                onDismiss = { dialog = null },
                onAdd = { subject, desc, cd, ed ->
                    scope.launch {
                        val realTask = taskList.CreateAndAddNewTaskWithReturn(subject, desc, cd, ed)
                        withContext(Dispatchers.IO) {
                            NotificationHelper.scheduleNotification(realTask, context)
                        }
                    }
                    dialog = null
                }
            )
        }

        //Если выбрарно удалить все задачи
        DialogType.DELETE_ALL -> {
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text("Удалить все задачи?", color = currentTextColor) },
                text = { Text("Задачи будут перемещены в архив.", color = currentTextColor) },
                confirmButton = {
                    Button(
                        onClick = {
                            moveAllToArchive()
                            dialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = currentButtonColor)
                    ) { Text("Удалить всё",  color = currentTextColor) }
                },
                dismissButton = {
                    TextButton(onClick = { dialog = null }) {
                        Text("Отмена",  color = currentTextColor)
                    }
                },
                containerColor = currentTopColor
            )
        }

        //Если нажал кнопку "Архив"
        DialogType.ARCHIVE -> {
            ArchiveDialog(
                archive = archiveTasks, // Передаем состояние
                onRestore = { task -> archive.RemoveFromArchive(task) },
                onDeleteForever = { task -> archive.DeleteForever(task) },
                onClearAll = { archive.DeleteEverything() },
                onDismiss = { dialog = null }
            )
        }

        //Если выбран экспорт в эксель
        //Лучше не лезть сюда, работает и ладно
        DialogType.EXPORT -> {
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text("Экспорт отчёта",  color = currentTextColor) },
                text = { Text("Создать файл с текущими задачами?", color = currentTextColor) },
                confirmButton = {Button(
                    onClick = {

                        scope.launch {
                            val uri = exportTasksToExcel(context, tasks) // tasks и context доступны тут
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setData(uri)
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                context.startActivity(Intent.createChooser(intent, "Открыть отчёт"))
                            }
                            dialog = null
                        }

                    },
                    colors = ButtonDefaults.buttonColors(Color(0xFF107C41), Color.White)
                ) { Text("Экспортировать") }},
                dismissButton = {
                    TextButton(onClick = {dialog = null} ) {
                        Text("Отмена", color = currentTextColor)
                    }
                }
                ,containerColor = currentTopColor)

        }

        //Если выбрано редактирование задачи
        DialogType.EDIT -> {
            val idx = selectedIndex
            val task = idx?.let { tasks.getOrNull(it) }
            if (task != null) {
                EditTaskDialog(
                    onAdd = { subject, desc, cd, ed ->
                        scope.launch(Dispatchers.IO) {
                            NotificationHelper.cancelNotification(task, context)
                            taskList.EditTask(task, subject, desc, cd, ed)
                            NotificationHelper.scheduleNotification(task, context)
                        } // НАДО ДОБАВИТЬ ЭТУ СТРОКУ
                        dialog = null
                    },
                    onDismiss = { dialog = null },
                    taskitself = task
                )
            }
        }

        //Обработка Null
        null -> {}
    }

}