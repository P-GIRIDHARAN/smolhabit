package com.example.smolhabits

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.smolhabits.data.Habit
import com.example.smolhabits.ui.theme.SmolhabitsTheme
import java.util.*

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handle if needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            SmolhabitsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SmolHabitsApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun SmolHabitsApp(modifier: Modifier = Modifier) {
    var currentHabit by remember { mutableStateOf<Habit?>(null) }
    var isCompletedToday by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (currentHabit == null) {
            HabitSelectionScreen(onHabitSelected = {
                currentHabit = it
                isCompletedToday = false
            })
        } else {
            HabitActiveScreen(
                habit = currentHabit!!,
                isCompletedToday = isCompletedToday,
                onHabitCompleted = { isCompletedToday = true },
                onClearHabit = { currentHabit = null }
            )
        }
    }
}

// ---------------- TIME PICKER DIALOG ----------------
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Reminder Time") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hour:")
                Spacer(Modifier.width(8.dp))
                TextField(
                    value = hour.toString(),
                    onValueChange = { h -> hour = h.toIntOrNull() ?: hour },
                    modifier = Modifier.width(60.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text("Minute:")
                Spacer(Modifier.width(8.dp))
                TextField(
                    value = minute.toString(),
                    onValueChange = { m -> minute = m.toIntOrNull() ?: minute },
                    modifier = Modifier.width(60.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onTimeSelected(hour, minute) }) { Text("Set") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ---------------- NOTIFICATION SCHEDULER ----------------
private fun schedulePushNotification(
    context: Context,
    habitName: String,
    hour: Int,
    minute: Int,
    delayInMinutes: Int? = null
) {
    val data = androidx.work.Data.Builder().putString("habitName", habitName).build()
    val workRequest = if (delayInMinutes != null) {
        androidx.work.OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayInMinutes.toLong(), java.util.concurrent.TimeUnit.MINUTES)
            .setInputData(data)
            .addTag("quick_reminder")
            .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
    } else {
        val now = Calendar.getInstance()
        val reminderTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        val delayMillis = reminderTime.timeInMillis - now.timeInMillis
        androidx.work.OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("daily_reminder")
            .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
    }

    val workManager = androidx.work.WorkManager.getInstance(context)
    if (delayInMinutes != null) {
        workManager.cancelAllWorkByTag("quick_reminder")
    } else {
        workManager.cancelAllWorkByTag("daily_reminder")
    }
    workManager.enqueue(workRequest)
}

// ---------------- SCREENS ----------------
@Composable
fun HabitSelectionScreen(onHabitSelected: (Habit) -> Unit) {
    val context = LocalContext.current
    var hour by remember { mutableStateOf(8) }
    var minute by remember { mutableStateOf(0) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Example habit card
    HabitSelectionCard(
        habit = Habit(
            id = "drink_water",
            name = "Drink Water",
            emoji = "🚰",
            description = "Stay hydrated"
        ),
        onClick = {
            onHabitSelected(
                Habit(
                    id = "drink_water",
                    name = "Drink Water",
                    emoji = "🚰",
                    description = "Stay hydrated"
                )
            )
        }
    )


    if (showTimePicker) {
        TimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onTimeSelected = { h, m ->
                hour = h
                minute = m
                schedulePushNotification(context, "Drink Water", h, m)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun HabitSelectionCard(habit: Habit, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = habit.emoji,
                fontSize = 32.sp,
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .wrapContentSize()
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = habit.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun HabitActiveScreen(
    habit: Habit,
    isCompletedToday: Boolean,
    onHabitCompleted: () -> Unit,
    onClearHabit: () -> Unit
) {
    val context = LocalContext.current
    var hour by remember { mutableStateOf(habit.reminderHour) }
    var minute by remember { mutableStateOf(habit.reminderMinute) }
    var showTimePicker by remember { mutableStateOf(false) }
    var quickTimerMinutes by remember { mutableStateOf("1") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(text = habit.emoji, fontSize = 72.sp)
        Text(
            text = habit.name,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium)
        )
        Text(
            text = habit.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        // Quick timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = quickTimerMinutes,
                onValueChange = { quickTimerMinutes = it },
                label = { Text("Minutes") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                quickTimerMinutes.toIntOrNull()?.let {
                    schedulePushNotification(context, habit.name, 0, 0, it)
                }
            }) {
                Text("Start Timer")
            }
        }

        Text("Or set daily reminder:")

        Button(onClick = { showTimePicker = true }) {
            Text("Set Reminder Time")
        }

        if (showTimePicker) {
            TimePickerDialog(
                initialHour = hour,
                initialMinute = minute,
                onTimeSelected = { h, m ->
                    hour = h; minute = m; showTimePicker = false
                    schedulePushNotification(context, habit.name, h, m)
                },
                onDismiss = { showTimePicker = false }
            )
        }

        // Complete button
        if (isCompletedToday) {
            Button(onClick = {}, enabled = false) { Text("Completed Today!") }
        } else {
            Button(onClick = onHabitCompleted) { Text("Mark Complete") }
        }

        TextButton(onClick = onClearHabit) { Text("Change Habit") }
    }
}
