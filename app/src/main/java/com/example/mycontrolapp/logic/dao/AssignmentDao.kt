package com.example.mycontrolapp.logic.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mycontrolapp.logic.Activity
import com.example.mycontrolapp.logic.Assignment
import com.example.mycontrolapp.logic.User
import com.example.mycontrolapp.logic.sharedData.AssignedCountRow
import com.example.mycontrolapp.logic.sharedEnums.Profession
import com.example.mycontrolapp.logic.sharedEnums.Team
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {
    // Return rowId so caller can know success; ABORT throws on conflict
    @Query("DELETE FROM assignments")
    suspend fun deleteAll()
    @Insert(onConflict = OnConflictStrategy.Companion.ABORT)
    suspend fun insert(assignment: Assignment): Long

    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("""
        SELECT activityId AS activityId,
               COUNT(*)     AS assigned
        FROM assignments
        GROUP BY activityId
    """)
    fun assignedCountsAllFlow(): Flow<List<AssignedCountRow>>

    @Query("""
        DELETE FROM assignments
        WHERE activityId = :activityId
          AND userId IN (SELECT id FROM users WHERE team = :team)
    """)
    suspend fun deleteAssignmentsForActivityByTeam(activityId: String, team: Team): Int

    @Query("DELETE FROM assignments WHERE activityId = :activityId AND userId = :userId")
    suspend fun delete(activityId: String, userId: String)

    // NEW: all assignments (flow + once)
    @Query("SELECT * FROM assignments")
    fun streamAll(): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments")
    suspend fun getAllOnce(): List<Assignment>

    @Query("SELECT * FROM assignments WHERE activityId = :activityId")
    fun streamByActivity(activityId: String): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments WHERE userId = :userId")
    fun streamByUser(userId: String): Flow<List<Assignment>>

    @Query("""
        SELECT u.* FROM users u
        INNER JOIN assignments a ON a.userId = u.id
        WHERE a.activityId = :activityId
        ORDER BY u.name
    """)
    fun streamUsersForActivity(activityId: String): Flow<List<User>>

    @Query("""
        SELECT act.* FROM activities act
        INNER JOIN assignments a ON a.activityId = act.id
        WHERE a.userId = :userId
        ORDER BY act.start_at
    """)
    fun streamActivitiesForUser(userId: String): Flow<List<Activity>>

    @Query("SELECT COUNT(*) FROM assignments WHERE activityId = :activityId AND role = :role")
    fun countByActivityAndRoleFlow(activityId: String, role: Profession): Flow<Int>

    @Query("SELECT * FROM assignments WHERE activityId = :activityId")
    fun assignmentsForActivityFlow(activityId: String): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments WHERE activityId = :activityId ORDER BY orderInActivity ASC")
    fun getAssignmentsForActivityOrdered(activityId: String): Flow<List<Assignment>>

    @Query("DELETE FROM assignments WHERE activityId = :activityId")
    suspend fun deleteByActivity(activityId: String)
}