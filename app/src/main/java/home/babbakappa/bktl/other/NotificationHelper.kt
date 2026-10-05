/* NotificationHelper.kt
* В этом файле написаны планирование, отмена и
* получение уведомления о задаче за день до
* дедлайна
*
* Потом переделаю так, чтобы можно было ставить время
* алярма какое захочет пользователь */

package home.babbakappa.bktl.other

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import home.babbakappa.bktl.core.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object NotificationHelper {
    const val CHANNEL_ID = "task_reminder_channel"
    private const val CHANNEL_NAME = "Напоминания о задачах"
    private const val CHANNEL_DESC = "Уведомления за день до задачи"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            Log.d("Notification", "Канал создан: ${channel.id}")
        }
        else {
            Log.e("Notification", "Канал не нужен (Android < 8)")
        }
    }

    fun scheduleNotification(task: Task, context: Context) {

        Log.d("Notification", "Планируем для задачи: ${task.subject_name}, дата ${task.creation_date}, дата дедлайна ${task.expire_date}")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationId = task.id.toInt()

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("subject", task.subject_name)
            putExtra("description", task.description)
            putExtra("date", task.creation_date)
            putExtra("dedldate", task.expire_date)
            putExtra("notification_id", notificationId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTime = getNotificationTime(task.expire_date)   // 00:00 дня "за день до дедлайна"
        val deadlineEndTime  = getDeadlineEndTime(task.expire_date)    // 23:59:59 дня дедлайна
        val now = System.currentTimeMillis()

        Log.d("Notification", "Время уведомления (мс): $notificationTime, конец дня дедлайна: $deadlineEndTime, текущее: $now")

        if (notificationTime == null || deadlineEndTime == null) {
            Log.w("Notification", "Не удалось распарсить даты. Уведомление не запланировано")
            return
        }

        val triggerTime: Long = when {
            // Время напоминания ещё не наступило — планируем ровно на него
            notificationTime > now -> notificationTime

            // Время напоминания уже прошло (мы внутри "дня до дедлайна"),
            // но дедлайн ещё не наступил — стреляем сразу
            deadlineEndTime > now -> now + 5_000L

            // Дедлайн уже прошёл — уведомление не нужно
            else -> {
                Log.w("Notification", "Дедлайн уже прошёл. Уведомление не запланировано")
                return
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (context.checkSelfPermission(Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
                    val alarmInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
                    alarmManager.setAlarmClock(alarmInfo, pendingIntent)
                    Log.d("Notification", "Уведомление запланировано через setAlarmClock на $triggerTime")
                    return
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d("Notification", "Уведомление успешно запланировано на $triggerTime (ID $notificationId)")
        }
        catch (error: SecurityException) {
            Log.e("Notification", "SecurityException при планировании: ${error.message}")
            try {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                Log.d("Notification", "Уведомление запланировано через setExact (fallback)")
            } catch (e2: SecurityException) {
                Log.e("Notification", "Ошибка и при fallback: ${e2.message}")
            }
        }
        catch (e: Exception) {
            Log.e("Notification", "Неизвестная ошибка: ${e.message}")
        }
    }

    // Конец дня дедлайна (23:59:59.999) — нужен, чтобы понять, наступил ли уже дедлайн
    private fun getDeadlineEndTime(dateStr: String): Long? {
        return try {
            val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val calendar = Calendar.getInstance().apply {
                time = format.parse("$dateStr") ?: return null
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            calendar.timeInMillis
        } catch (e: Exception) {
            Log.e("Notification", "Ошибка парсинга даты дедлайна: $dateStr", e)
            null
        }
    }

    fun cancelNotification(task: Task, context: Context) {
        Log.d("Notification", "Отменяем уведомление для ${task.subject_name}")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, task.id.toInt(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } else {
            Log.d("Notification", "PendingIntent не найден — уведомление и не было запланировано")
        }
        NotificationManagerCompat.from(context).cancel(task.id.toInt())
    }

    fun rescheduleAll(tasks: List<Task>, context: Context) {
        tasks.forEach { cancelNotification(it, context) }
        tasks.forEach { scheduleNotification(it, context) }
    }

    private fun getNotificationTime(dateStr: String): Long? {
        return try {
            val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val calendar = Calendar.getInstance().apply {
                time = format.parse("$dateStr") ?: return null
                add(Calendar.DAY_OF_YEAR, -1)
            }
            Log.d("Notification", "Распарсено время: ${calendar.time}")
            calendar.timeInMillis
        } catch (e: Exception) {
            Log.e("Notification", "Ошибка парсинга даты: $dateStr", e)
            null
        }
    }
}