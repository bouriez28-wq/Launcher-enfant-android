package com.example.launchertlphoneenfant.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.launchertlphoneenfant.data.model.RewardClaimHistory
import com.example.launchertlphoneenfant.data.model.RewardItem
import com.example.launchertlphoneenfant.data.model.StickerMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardItemDao {
    @Query("SELECT * FROM reward_items ORDER BY starCost ASC")
    fun getAllRewards(): Flow<List<RewardItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReward(reward: RewardItem)

    @Update
    suspend fun updateReward(reward: RewardItem)

    @Delete
    suspend fun deleteReward(reward: RewardItem)

    @Query("SELECT * FROM reward_claim_history ORDER BY timestamp DESC")
    fun getClaimHistory(): Flow<List<RewardClaimHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClaimHistory(claim: RewardClaimHistory)

    @Query("SELECT * FROM sticker_messages ORDER BY timestamp DESC")
    fun getStickerMessages(): Flow<List<StickerMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStickerMessage(message: StickerMessage)
}
