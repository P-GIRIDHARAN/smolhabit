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
class MainActivity : ComponentActivity() {
    
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handle permission result if needed
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Request notification permission for Android 13+
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
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Simple Compose time picker dialog
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
            Button(onClick = { onTimeSelected(hour, minute) }) {
                Text("Set")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun scheduleHabitReminder(context: Context, habitName: String, hour: Int, minute: Int) {
    val now = java.util.Calendar.getInstance()
    val reminderTime = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, hour)
        set(java.util.Calendar.MINUTE, minute)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
        if (before(now)) add(java.util.Calendar.DAY_OF_MONTH, 1)
    }
    val delayMillis = reminderTime.timeInMillis - now.timeInMillis
    val data = androidx.work.Data.Builder()
        .putString("habitName", habitName)
        .build()
    val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.smolhabits.ReminderWorker>()
        .setInitialDelay(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
        .setInputData(data)
        .build()
    androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
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
        when {
            currentHabit == null -> {
                HabitSelectionScreen(
                    onHabitSelected = { habit ->
                        currentHabit = habit
                        isCompletedToday = false
                    }
                )
            }
            else -> {
                HabitActiveScreen(
                    habit = currentHabit!!,
                    isCompletedToday = isCompletedToday,
                    onHabitCompleted = { isCompletedToday = true },
                    onClearHabit = { currentHabit = null }
                )
            }
        }
    }
}

@Composable
private fun HabitSelectionScreen(onHabitSelected: (Habit) -> Unit) {
    val habits = listOf(
        Habit("drink_water", "Drink Water", "", "Stay hydrated with a glass of water"),
        Habit("stretch", "Stretch", "", "Take a moment to stretch your body"),
        Habit("meditate", "Meditate", "", "Find peace with a few minutes of meditation")
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text(
            text = "Choose Your\nDaily Focus",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Light,
                lineHeight = 36.sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "Pick one habit to focus on today.\nSimple. Effective. Sustainable.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        habits.forEach { habit ->
            HabitSelectionCard(
                habit = habit,
                onClick = { onHabitSelected(habit) }
            )
        }
    }
}

@Composable
private fun HabitSelectionCard(habit: Habit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Header with habit info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = habit.emoji, fontSize = 72.sp)
            Text(
                text = habit.name,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = habit.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        
        // Timer UI
        var hour by remember { mutableStateOf(habit.reminderHour) }
        var minute by remember { mutableStateOf(habit.reminderMinute) }
        var showTimePicker by remember { mutableStateOf(false) }
        val context = LocalContext.current
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Set Reminder Time: %02d:%02d".format(hour, minute))
        }
        if (showTimePicker) {
            TimePickerDialog(
                initialHour = hour,
                initialMinute = minute,
                onTimeSelected = { h, m ->
                    hour = h
                    minute = m
                    showTimePicker = false
                    // Schedule notification
                    scheduleHabitReminder(context, habit.name, h, m)
                },
                onDismiss = { showTimePicker = false }
            )
        }
        
        // Complete button
        if (isCompletedToday) {
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                enabled = false
            ) {
                Text("Completed Today! ", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
            }
        } else {
            Button(
                onClick = onHabitCompleted,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Mark Complete ", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
            }
        }
        
        // Change habit button
        TextButton(
            onClick = onClearHabit,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        ) {
            Text("Change Habit")
        }
    }
}
