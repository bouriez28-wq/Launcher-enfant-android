package com.example.launchertlphoneenfant.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.launchertlphoneenfant.data.model.BedtimeStory
import kotlinx.coroutines.flow.Flow

@Dao
interface BedtimeStoryDao {
    @Query("SELECT * FROM bedtime_stories ORDER BY id ASC")
    fun getAllStories(): Flow<List<BedtimeStory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: BedtimeStory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStories(stories: List<BedtimeStory>)

    @Update
    suspend fun updateStory(story: BedtimeStory)

    @Delete
    suspend fun deleteStory(story: BedtimeStory)
}
