package com.example.launchertlphoneenfant.parent.model

import java.util.UUID

data class RoutineItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val icon: String = "⭐",
    val starsReward: Int = 1,
    val isCompleted: Boolean = false,
    val isPendingValidation: Boolean = false,
    val orderIndex: Int = 0
)

data class RewardClaimHistory(
    val id: String = UUID.randomUUID().toString(),
    val rewardTitle: String = "",
    val rewardIcon: String = "🎁",
    val starCost: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isPendingApproval: Boolean = true
)
