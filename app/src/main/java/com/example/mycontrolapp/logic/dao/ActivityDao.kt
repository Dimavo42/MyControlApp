package com.example.mycontrolapp.logic.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mycontrolapp.logic.Activity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsert(activity: Activity)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsertAll(activities: List<Activity>)

    @Update
    suspend fun update(activity: Activity): Int

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM activities")
    suspend fun deleteAll()

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getById(id: String): Activity?

    // NEW: all activities (flow + once)
    @Query("SELECT * FROM activities ORDER BY start_at")
    fun streamAll(): Flow<List<Activity>>

    @Query("SELECT * FROM activities ORDER BY start_at")
    suspend fun getAllOnce(): List<Activity>

    // Existing day/overlap helpers
    @Query("""
        SELECT * FROM activities
        WHERE date_epoch_day = :epochDay
        ORDER BY start_at
    """)
    fun streamOnDay(epochDay: Int): Flow<List<Activity>>

    @Query("""
        SELECT * FROM activities
        WHERE date_epoch_day = :epochDay
          AND start_at < :reqEndAt
          AND end_at   > :reqStartAt
        ORDER BY start_at
    """)
    suspend fun findOverlappingOnDay(
        epochDay: Int,
        reqStartAt: Long,
        reqEndAt: Long
    ): List<Activity>

    @Query("""
        SELECT * FROM activities
        WHERE start_at < :to AND end_at > :from
        ORDER BY start_at
    """)
    suspend fun findOverlappingAbsolute(from: Long, to: Long): List<Activity>

    @Query("""
        SELECT * FROM activities
        WHERE start_at <= :now AND end_at > :now
        ORDER BY start_at
    """)
    fun streamActiveNow(now: Long): Flow<List<Activity>>


}