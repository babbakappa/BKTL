package home.babbakappa.bktl

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

//Диалог добавления задачи
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, String, String, String) -> Unit) {

    //Ебучка цвет рамок и текста полей ввода
    val need_colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SetTextColor(),       // Цвет рамки при фокусе
        unfocusedBorderColor = SetBGColor(),     // Цвет рамки без фокуса
        focusedLabelColor = SetTextColor(),        // Цвет текста подсказки при фокусе
        unfocusedLabelColor = SetBGColor(),       // Цвет текста подсказки без фокуса
        focusedTextColor = SetTextColor(),
        unfocusedTextColor = SetTextColor(),
        cursorColor = SetTextColor()
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

    var expanded by remember { mutableStateOf(false) }

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


                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), // Отступ между полем и кнопкой
                    verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Предмет", color = SetTextColor()) },
                        modifier = Modifier.weight(1f),
                        colors = need_colors,
                    )

                }


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
                Button(onClick = { showValidationError = false }, colors = ButtonDefaults.buttonColors(containerColor = SetButtonColor())) {
                    Text("ОК", color = SetTextColor())
                }
            },
            containerColor = SetTopColor()
        )
    }
}


//Функция для редактирования задачи
@Composable
@OptIn(ExperimentalMaterial3Api::class)
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

    var expanded by remember { mutableStateOf(false) }


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
        unfocusedTextColor = SetTextColor(),
        cursorColor = SetTextColor()
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
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), // Отступ между полем и кнопкой
                    verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Предмет", color = SetTextColor()) },
                        modifier = Modifier.weight(1f),
                        colors = need_colors,
                    )

                }
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

    //Вывод месседжбокса при вводе пустых данных
    if (showValidationError) {
        AlertDialog(
            onDismissRequest = { showValidationError = false },
            title = { Text("Ошибка заполнения", color = SetTextColor()) },
            text = { Text("Пожалуйста, заполните все поля формы перед сохранением.", color = SetTextColor()) },
            confirmButton = {
                Button(onClick = { showValidationError = false }, colors = ButtonDefaults.buttonColors(containerColor = SetButtonColor())) {
                    Text("ОК", color = SetTextColor())
                }
            },
            containerColor = SetTopColor()
        )
    }
}
