package home.babbakappa.bktl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("Notification", "onReceive вызван! Intent: ${intent.action}")

        val subject = intent.getStringExtra("subject") ?: "Задача"
        Log.d("Notification", "Предмет: $subject, ID: ${intent.getIntExtra("notification_id", 0)}")
        val description = intent.getStringExtra("description") ?: ""
        val date = intent.getStringExtra("dedldate") ?: ""
        val notificationId = intent.getIntExtra("notification_id", 0)

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_foreground)
            .setContentTitle("Напоминание: $subject")
            .setContentText("Задача на $date${if (description.isNotEmpty() && description != "None") "\n$description" else ""}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.d("Notification", "Уведомление показано (ID $notificationId)")
        }
        catch (error: SecurityException) {
            Log.e("Notification", "Не удалось показать уведомление: ${error.message}")
        }
    }
}