package com.example.launchertlphoneenfant.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.launchertlphoneenfant.data.dao.AppRestrictionDao
import com.example.launchertlphoneenfant.data.dao.BedtimeStoryDao
import com.example.launchertlphoneenfant.data.dao.RewardDao
import com.example.launchertlphoneenfant.data.dao.RewardItemDao
import com.example.launchertlphoneenfant.data.dao.RoutineDao
import com.example.launchertlphoneenfant.data.dao.RoutineItemDao
import com.example.launchertlphoneenfant.data.model.AppRestriction
import com.example.launchertlphoneenfant.data.model.BedtimeStory
import com.example.launchertlphoneenfant.data.model.Reward
import com.example.launchertlphoneenfant.data.model.RewardClaimHistory
import com.example.launchertlphoneenfant.data.model.RewardItem
import com.example.launchertlphoneenfant.data.model.Routine
import com.example.launchertlphoneenfant.data.model.RoutineItem
import com.example.launchertlphoneenfant.data.model.StickerMessage

@Database(
    entities = [
        AppRestriction::class,
        Routine::class,
        Reward::class,
        RoutineItem::class,
        RewardItem::class,
        RewardClaimHistory::class,
        StickerMessage::class,
        BedtimeStory::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appRestrictionDao(): AppRestrictionDao
    abstract fun routineDao(): RoutineDao
    abstract fun rewardDao(): RewardDao
    abstract fun routineItemDao(): RoutineItemDao
    abstract fun rewardItemDao(): RewardItemDao
    abstract fun bedtimeStoryDao(): BedtimeStoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "launcher_enfant_db"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
