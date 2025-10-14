package com.example.mycontrolapp.logic.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mycontrolapp.logic.ActivityRoleRequirement
import com.example.mycontrolapp.logic.sharedData.RequiredCountRow
import com.example.mycontrolapp.logic.sharedEnums.Profession

@Dao
interface ActivityRoleRequirementDao {

    @Query("""
        SELECT activityId           AS activityId,
               SUM(requiredCount)   AS required
        FROM activity_role_requirements
        GROUP BY activityId
    """)
    fun requiredCountsAllFlow(): kotlinx.coroutines.flow.Flow<List<RequiredCountRow>>
    @Query("SELECT * FROM activity_role_requirements WHERE activityId = :activityId")
    fun requirementsForActivityFlow(activityId: String): kotlinx.coroutines.flow.Flow<List<ActivityRoleRequirement>>

    @Query("SELECT * FROM activity_role_requirements WHERE activityId = :activityId")
    suspend fun requirementsForActivity(activityId: String): List<ActivityRoleRequirement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ActivityRoleRequirement>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ActivityRoleRequirement)

    @Query("DELETE FROM activity_role_requirements WHERE activityId = :activityId AND profession = :profession")
    suspend fun deleteRequirement(activityId: String, profession: Profession)

    @Query("DELETE FROM activity_role_requirements WHERE activityId = :activityId")
    suspend fun deleteAllForActivity(activityId: String)

    @Query("DELETE FROM activity_role_requirements")
    suspend fun deleteAll()
}