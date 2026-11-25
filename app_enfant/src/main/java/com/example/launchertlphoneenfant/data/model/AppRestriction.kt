package com.example.launchertlphoneenfant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_restrictions")
data class AppRestriction(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isHidden: Boolean = false,
    val category: String = "Général"
)
