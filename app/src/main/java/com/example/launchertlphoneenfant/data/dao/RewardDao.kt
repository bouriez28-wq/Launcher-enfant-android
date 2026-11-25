package com.example.launchertlphoneenfant.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.launchertlphoneenfant.data.model.Reward
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardDao {
    @Query("SELECT * FROM rewards ORDER BY id ASC")
    fun getAllRewards(): Flow<List<Reward>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reward: Reward)

    @Update
    suspend fun update(reward: Reward)

    @Delete
    suspend fun delete(reward: Reward)
}
