package com.example.launchertlphoneenfant.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.launchertlphoneenfant.data.model.RoutineItem
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineItemDao {
    @Query("SELECT * FROM routine_items ORDER BY orderIndex ASC")
    fun getAllRoutines(): Flow<List<RoutineItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(routine: RoutineItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(routines: List<RoutineItem>)

    @Update
    suspend fun update(routine: RoutineItem)

    @Delete
    suspend fun delete(routine: RoutineItem)

    @Query("UPDATE routine_items SET isCompleted = 0, isPendingValidation = 0")
    suspend fun resetAllRoutines()
}
