package com.example.mycontrolapp.logic.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mycontrolapp.logic.ActivityTimeSplit
import kotlinx.coroutines.flow.Flow


@Dao
interface ActivityTimeSplitDao {

    @Query("SELECT * FROM activity_time_split WHERE activityId = :activityId LIMIT 1")
    fun observe(activityId: String): Flow<ActivityTimeSplit?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ActivityTimeSplit)

    @Query("DELETE FROM activity_time_split WHERE activityId = :activityId")
    suspend fun clear(activityId: String)
}