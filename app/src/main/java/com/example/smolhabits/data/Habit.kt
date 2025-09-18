package com.example.smolhabits.data

import kotlinx.coroutines.flow.Flow

data class Habit(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val isActive: Boolean = false,
    val lastCompletedDate: String? = null,
    val currentStreak: Int = 0,
    val totalCompletions: Int = 0,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0
)

// Predefined habits
object HabitTemplates {
    val DRINK_WATER = Habit(
        id = "drink_water",
        name = "Drink Water",
        emoji = "",
        description = "Stay hydrated with a glass of water"
    )
    
    val STRETCH = Habit(
        id = "stretch", 
        name = "Stretch",
        emoji = "",
        description = "Take a moment to stretch your body"
    )
    
    val MEDITATE = Habit(
        id = "meditate",
        name = "Meditate", 
        emoji = "",
        description = "Find peace with a few minutes of meditation"
    )
    
    val ALL = listOf(DRINK_WATER, STRETCH, MEDITATE)
}

interface HabitRepository {
    suspend fun getCurrentHabit(): Habit?
    suspend fun setCurrentHabit(habit: Habit?)
    suspend fun markHabitComplete(date: String)
    suspend fun updateHabitReminder(hour: Int, minute: Int)
    fun getCurrentHabitFlow(): Flow<Habit?>
}
