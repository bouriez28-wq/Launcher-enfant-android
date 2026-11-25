package com.example.launchertlphoneenfant.data.model

data class Story(
    val id: String = "",
    val title: String,
    val content: String,
    val durationMinutes: Int = 5,
    val category: String = "Conte de Fées",
    val iconEmoji: String = "🌙"
)
