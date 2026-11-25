package com.example.launchertlphoneenfant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class TimeOfDay(val displayName: String, val icon: String) {
    MORNING("Matin", "🌅"),
    AFTERNOON("Après-midi", "☀️"),
    EVENING("Soir", "🌙")
}

enum class RoutineFrequency(val displayName: String, val icon: String) {
    DAILY("Quotidienne", "🔄"),
    WEEKLY("Hebdomadaire", "📅"),
    MONTHLY("Mensuelle", "📆")
}

@Entity(tableName = "routine_items")
data class RoutineItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val icon: String = "⭐",
    val timeOfDay: TimeOfDay = TimeOfDay.MORNING,
    val starsReward: Int = 1,
    val isCompleted: Boolean = false,
    val completedDate: String = "",
    val isPendingValidation: Boolean = false,
    val frequency: RoutineFrequency = RoutineFrequency.DAILY,
    val orderIndex: Int = 0
)

@Entity(tableName = "reward_items")
data class RewardItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val icon: String = "🎁",
    val starCost: Int = 5,
    val timesClaimed: Int = 0
)

@Entity(tableName = "reward_claim_history")
data class RewardClaimHistory(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val rewardTitle: String,
    val rewardIcon: String,
    val starCost: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isPendingApproval: Boolean = true
)

@Entity(tableName = "sticker_messages")
data class StickerMessage(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val icon: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isReadByChild: Boolean = false
)

@Entity(tableName = "bedtime_stories")
data class BedtimeStory(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val durationMinutes: Int = 5,
    val category: String = "Conte de Fées",
    val iconEmoji: String = "🌙"
)
