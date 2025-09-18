package com.example.smolhabits

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val habitName = inputData.getString("habitName") ?: "Habit"
        sendNotification(habitName)
        return Result.success()
    }

    private fun sendNotification(habitName: String) {
        val channelId = "habit_reminder_channel"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Habit Reminder")
            .setContentText("Time to do: $habitName")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .build()

        notificationManager.notify(1, notification)
    }
}
