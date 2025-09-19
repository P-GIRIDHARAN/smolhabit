package com.example.smolhabits.notification

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import androidx.core.app.NotificationCompat
import com.example.smolhabits.MainActivity

class PomodoroService : Service() {

    private var timer: CountDownTimer? = null
    private val channelId = "pomodoro_channel"
    private val notificationId = 1001

    // Variables for work/rest durations and current phase
    private var workMillis: Long = 25 * 60 * 1000L
    private var restMillis: Long = 5 * 60 * 1000L
    private var phase: String = "Work"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Get durations from intent
        workMillis = intent?.getLongExtra("workMillis", 25 * 60 * 1000L) ?: 25 * 60 * 1000L
        restMillis = intent?.getLongExtra("restMillis", 5 * 60 * 1000L) ?: 5 * 60 * 1000L

        startWorkPhase() // Always start with Work phase
        return START_NOT_STICKY
    }

    private fun startWorkPhase() {
        phase = "Work"
        startCountdown(workMillis)
    }

    private fun startRestPhase() {
        phase = "Rest"
        startCountdown(restMillis)
    }

    private fun startCountdown(durationMillis: Long) {
        timer?.cancel()
        timer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                val timeLeft = String.format("%02d:%02d", minutes, seconds)
                showNotification("$phase time left: $timeLeft")
            }

            override fun onFinish() {
                if (phase == "Work") startRestPhase() else startWorkPhase()
            }
        }.start()
    }

    private fun showNotification(timeText: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Pomodoro Timer")
            .setContentText(timeText)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(notificationId, notification) // Must call this immediately
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pomodoro Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
