package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.CharacterProfileEntity
import com.example.data.model.VideoProjectEntity
import com.example.data.model.VideoSceneEntity

@Database(
    entities = [
        VideoProjectEntity::class,
        CharacterProfileEntity::class,
        VideoSceneEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoProjectDao(): VideoProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_video_creator_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
