package com.example.smolhabits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smolhabits.notification.PomodoroService
import com.example.smolhabits.ui.theme.SmolhabitsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmolhabitsTheme {
                Scaffold { innerPadding ->
                    PomodoroApp(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun PomodoroApp(modifier: Modifier = Modifier) {
    var workMinutes by remember { mutableStateOf("") }
    var restMinutes by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Pomodoro Timer", fontSize = 32.sp)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = workMinutes,
            onValueChange = { workMinutes = it },
            label = { Text("Work minutes") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = restMinutes,
            onValueChange = { restMinutes = it },
            label = { Text("Rest minutes") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        if (!isRunning) {
            Button(onClick = {
                val work = workMinutes.toIntOrNull() ?: 0
                val rest = restMinutes.toIntOrNull() ?: 0
                if (work > 0 && rest > 0) {
                    startPomodoro(context, work, rest)
                    isRunning = true
                }
            }) {
                Text("Start Pomodoro")
            }
        } else {
            Button(onClick = {
                stopPomodoro(context)
                isRunning = false
            }) {
                Text("Stop Pomodoro")
            }
        }
    }
}

// ------------------ ALARM SCHEDULING ------------------
// Start Pomodoro service instead of scheduling Alarm
fun startPomodoro(context: Context, workMinutes: Int, restMinutes: Int) {
    val intent = Intent(context, PomodoroService::class.java)
    intent.putExtra("workMillis", workMinutes * 60 * 1000L)
    intent.putExtra("restMillis", restMinutes * 60 * 1000L)
    context.startForegroundService(intent)

}

fun stopPomodoro(context: Context) {
    context.stopService(Intent(context, PomodoroService::class.java))
}
