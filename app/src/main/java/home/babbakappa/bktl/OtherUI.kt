package home.babbakappa.bktl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
