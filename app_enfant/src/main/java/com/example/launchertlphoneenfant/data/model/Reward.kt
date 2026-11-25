package com.example.launchertlphoneenfant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rewards")
data class Reward(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val costMinutes: Int = 30,
    val iconName: String = "gift",
    val isClaimed: Boolean = false
)
