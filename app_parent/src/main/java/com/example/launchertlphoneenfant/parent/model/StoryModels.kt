package com.example.launchertlphoneenfant.parent.model

import java.util.UUID

data class ChildProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val birthDateMillis: Long = 0L
) {
    fun getAgeInYears(): Int {
        if (birthDateMillis == 0L) return 4 // Âge par défaut
        val diffMillis = System.currentTimeMillis() - birthDateMillis
        return (diffMillis / (1000L * 60 * 60 * 24 * 365)).toInt()
    }
}

data class WeeklyTheme(
    val dayOfWeek: Int = 1, // 1 = Lundi, 7 = Dimanche
    val label: String = "",
    val promptTheme: String = ""
)

data class AiSettings(
    val apiKey: String = "",
    val modelName: String = "llama3-8b-8192"
)
