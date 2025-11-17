package com.example.mycontrolapp.logic
import com.example.mycontrolapp.logic.sharedData.AssignedCountRow
import com.example.mycontrolapp.logic.sharedData.RequiredCountRow
import com.example.mycontrolapp.logic.sharedData.TimeSegment
import com.example.mycontrolapp.logic.sharedEnums.Profession
import kotlinx.coroutines.flow.Flow

interface ListManager {
    // Activities
    fun activitiesFlow(): Flow<List<Activity>>
    fun activitiesOnDayFlow(epochDay: Int): Flow<List<Activity>>
    fun activeNowFlow(nowUtcMillis: Long): Flow<List<Activity>>
    suspend fun getActivities(): List<Activity>
    suspend fun addActivity(activity: Activity)
    suspend fun updateActivity(activity: Activity): Boolean
    suspend fun removeActivity(id: String)

    // Users
    fun usersFlow(): Flow<List<User>>
    suspend fun getUsers(): List<User>
    suspend fun addUser(user: User)
    suspend fun updateUser(user: User)
    suspend fun removeUser(id: String)

    // User ↔ Profession (M:N)
    fun professionsForUserFlow(userId: String): Flow<List<Profession>>
    fun usersByProfessionFlow(profession: Profession): Flow<List<User>>
    suspend fun setUserProfessions(userId: String, professions: Set<Profession>)

    // Assignments
    fun assignmentsFlow(): Flow<List<Assignment>>
    fun assignmentsByActivityFlow(activityId: String): Flow<List<Assignment>>
    fun assignmentsByUserFlow(userId: String): Flow<List<Assignment>>
    suspend fun getAssignments(): List<Assignment>
    suspend fun addAssignment(assignment: Assignment): Boolean
    suspend fun removeAssignment(assignmentId: String)

    suspend fun replaceAssignmentsForActivity(activityId: String,newAssignments: List<Assignment>)

    // Handy counters for UI
    fun assignedCountForActivityFlow(activityId: String): Flow<Int>

    fun requiredCountForActivityFlow(activityId: String): Flow<Int>

    // Activity role requirements
    fun roleRequirementsFlow(activityId: String): Flow<List<ActivityRoleRequirement>>
    suspend fun requirementsForActivity(activityId: String): List<ActivityRoleRequirement>
    suspend fun upsertAllRequirements(items: List<ActivityRoleRequirement>)
    suspend fun upsertRequirement(item: ActivityRoleRequirement)
    suspend fun deleteRequirement(activityId: String, profession: Profession)
    suspend fun deleteAllRequirementsForActivity(activityId: String)

    // Sum of all required seats for an activity (for “assigned/required” UI)

    fun userProfessionsFlow(userId: String): Flow<Set<Profession>>
    suspend fun replaceUserProfessions(userId: String, professions: Set<Profession>)


    fun assignedCountsAllFlow(): Flow<List<AssignedCountRow>>

    fun requiredCountsAllFlow(): Flow<List<RequiredCountRow>>


    fun timeSplitState(activityId: String): Flow<ActivityTimeSplit?>

    suspend fun saveTimeSplitState(
        activityId: String,
        segments: List<TimeSegment>,
        splitMinutes: Int
    )

    suspend fun clearTimeSplitState(activityId: String)


    suspend fun syncOnceIfRemoteEnabled(): Boolean = false

}


