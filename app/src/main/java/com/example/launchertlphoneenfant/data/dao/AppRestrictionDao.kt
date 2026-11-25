package com.example.launchertlphoneenfant.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.launchertlphoneenfant.data.model.AppRestriction
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRestrictionDao {
    @Query("SELECT * FROM app_restrictions")
    fun getAllRestrictions(): Flow<List<AppRestriction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(restriction: AppRestriction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(restrictions: List<AppRestriction>)

    @Update
    suspend fun update(restriction: AppRestriction)

    @Query("DELETE FROM app_restrictions WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
}
