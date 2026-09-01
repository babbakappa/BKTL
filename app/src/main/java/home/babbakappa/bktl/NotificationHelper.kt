package home.babbakappa.bktl

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

        Log.d("Notification", "Планируем для задачи: ${task.GetSubjectName()}, дата ${task.GetCreationDate()}, дата дедлайна ${task.GetExpireDate()}")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationId = task.hashCode()

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("subject", task.GetSubjectName())
            putExtra("description", task.GetDescription())
            putExtra("date", task.GetCreationDate())
            putExtra("dedldate", task.GetExpireDate())
            putExtra("notification_id", notificationId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val notificationTime = getNotificationTime(task.GetExpireDate())
        Log.d("Notification", "Время уведомления (мс): $notificationTime, текущее: ${System.currentTimeMillis()}")
        /*if (notificationTime == null || notificationTime < System.currentTimeMillis()) {
            Log.w("Notification", "Время невалидно или уже прошло. Уведомление не запланировано")
            return // Неверный формат или время уже прошло
        }*/

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Проверяем наличие разрешения
                if (context.checkSelfPermission(android.Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
                    // Используем AlarmClock как fallback
                    val alarmInfo = AlarmManager.AlarmClockInfo(notificationTime!!, pendingIntent)
                    alarmManager.setAlarmClock(alarmInfo, pendingIntent)
                    Log.d("Notification", "Уведомление запланировано через setAlarmClock (без точного разрешения)")
                    return
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    notificationTime!!,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime!!, pendingIntent)
            }
            Log.d("Notification", "Уведомление успешно запланировано (ID $notificationId)")
        }
        catch (error: SecurityException) {
            Log.e("Notification", "SecurityException при планировании: ${error.message}")

            try {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime!!, pendingIntent)
                Log.d("Notification", "Уведомление запланировано через setExact (fallback)")
            } catch (e2: SecurityException) {
                Log.e("Notification", "Ошибка и при fallback: ${e2.message}")
            }
        }
        catch (e: Exception) {
            Log.e("Notification", "Неизвестная ошибка: ${e.message}")
        }

    }

    fun cancelNotification(task: Task, context: Context) {
        Log.d("Notification", "Отменяем уведомление для ${task.GetSubjectName()}")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        NotificationManagerCompat.from(context).cancel(task.hashCode())
        Log.d("Notification", "Уведомление отменено (ID ${task.hashCode()})")
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