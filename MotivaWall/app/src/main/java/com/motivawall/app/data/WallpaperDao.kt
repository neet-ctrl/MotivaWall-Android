package com.motivawall.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {
    @Query("SELECT * FROM wallpaper_history ORDER BY dateSet DESC LIMIT 50")
    fun observeHistory(): Flow<List<WallpaperHistory>>

    @Query("SELECT * FROM wallpaper_history WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): WallpaperHistory?

    @Query("SELECT * FROM schedules WHERE id = :id LIMIT 1")
    suspend fun findScheduleById(id: Long): WallpaperSchedule?

    @Insert
    suspend fun insert(item: WallpaperHistory): Long

    @Update
    suspend fun update(item: WallpaperHistory)

    @Delete
    suspend fun delete(item: WallpaperHistory)

    @Query("DELETE FROM wallpaper_history")
    suspend fun clear()

    @Query("SELECT * FROM schedules ORDER BY time ASC")
    fun observeSchedules(): Flow<List<WallpaperSchedule>>

    @Insert
    suspend fun insertSchedule(schedule: WallpaperSchedule): Long

    @Update
    suspend fun updateSchedule(schedule: WallpaperSchedule)

    @Delete
    suspend fun deleteSchedule(schedule: WallpaperSchedule)
}