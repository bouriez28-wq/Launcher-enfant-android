package com.example.launchertlphoneenfant.parent.model

import java.util.UUID

enum class TimeOfDay(val displayName: String, val icon: String) {
    MORNING("Matin", "🌅"),
    AFTERNOON("Après-midi", "☀️"),
    EVENING("Soir", "🌙")
}

enum class RoutineFrequency(val displayName: String, val icon: String) {
    DAILY("Quotidienne", "🔄"),
    WEEKLY("Hebdomadaire", "📅"),
    MONTHLY("Mensuelle", "📆"),
    SEASONAL("Saisonnire", "🍂")
}

enum class Season(val displayName: String, val icon: String) {
    SPRING("Printemps", "🌸"),
    SUMMER("Été", "☀️"),
    AUTUMN("Automne", "🍂"),
    WINTER("Hiver", "❄️")
}

data class RoutineItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val icon: String = "⭐",
    val timeOfDay: TimeOfDay = TimeOfDay.MORNING,
    val starsReward: Int = 1,
    val isCompleted: Boolean = false,
    val completedDate: String = "",
    val isPendingValidation: Boolean = false,
    val frequency: RoutineFrequency = RoutineFrequency.DAILY,
    val season: Season? = null,
    val orderIndex: Int = 0
)

data class RewardItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val icon: String = "🎁",
    val starCost: Int = 5,
    val timesClaimed: Int = 0
)

data class RewardClaimHistory(
    val id: String = UUID.randomUUID().toString(),
    val rewardTitle: String = "",
    val rewardIcon: String = "🎁",
    val starCost: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isPendingApproval: Boolean = true
)

data class StickerMessage(
    val id: String = UUID.randomUUID().toString(),
    val icon: String = "⭐",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isReadByChild: Boolean = false
)

data class StarHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val amount: Int = 0,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
