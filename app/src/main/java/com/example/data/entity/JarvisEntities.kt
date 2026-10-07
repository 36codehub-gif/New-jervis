package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawCommand: String,
    val intentName: String,
    val status: String,
    val resultMessage: String,
    val timestamp: Long = System.currentTimeMillis(),
    val executionDurationMs: Long = 0
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val triggerPhrase: String,
    val description: String,
    val stepsJson: String,
    val isEnabled: Boolean = true,
    val iconName: String = "bolt"
)
