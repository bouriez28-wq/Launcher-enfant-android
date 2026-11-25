package com.example.launchertlphoneenfant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val rewardMinutes: Int = 15,
    val isCompleted: Boolean = false,
    val iconName: String = "star"
)
