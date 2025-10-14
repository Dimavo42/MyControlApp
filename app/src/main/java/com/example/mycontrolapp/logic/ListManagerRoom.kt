package com.example.mycontrolapp.logic
import androidx.room.withTransaction
import com.example.mycontrolapp.logic.sharedEnums.Profession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class ListManagerRoom @Inject constructor(
    private val db: AppDb
) : ListManager {

    /* -------------------- Activities -------------------- */


    override fun activitiesFlow(): Flow<List<Activity>> =
        db.activityDao().streamAll()

    override fun activitiesOnDayFlow(epochDay: Int): Flow<List<Activity>> =
        db.activityDao().streamOnDay(epochDay)

    override fun activeNowFlow(nowUtcMillis: Long): Flow<List<Activity>> =
        db.activityDao().streamActiveNow(nowUtcMillis)

    override suspend fun getActivities(): List<Activity> =
        db.activityDao().getAllOnce()

    override suspend fun addActivity(activity: Activity) {
        db.activityDao().upsert(activity)
    }

    override suspend fun updateActivity(activity: Activity): Boolean =
        db.activityDao().update(activity) > 0

    override suspend fun removeActivity(id: String) {
        db.activityDao().deleteById(id)
    }

    /* ---------------------- Users ----------------------- */

    override fun usersFlow(): Flow<List<User>> =
        db.userDao().getAllFlow()

    override suspend fun getUsers(): List<User> =
        db.userDao().getAllOnce()

    override suspend fun addUser(user: User) {
        db.userDao().upsert(user)
    }

    override suspend fun updateUser(user: User) {
        db.userDao().update(user)
    }

    override suspend fun removeUser(id: String) {
        db.userDao().deleteById(id)
    }


    /* -------- User ↔ Profession (cross-ref) ------------- */



    override fun professionsForUserFlow(userId: String): Flow<List<Profession>> =
        db.userProfessionDao().professionsForUserFlow(userId)

    override fun usersByProfessionFlow(profession: Profession): Flow<List<User>> =
        db.userProfessionDao().usersByProfessionFlow(profession)

    override suspend fun setUserProfessions(userId: String, professions: Set<Profession>) {
        db.userProfessionDao().setUserProfessions(userId, professions)
    }

    /* ------------------- Assignments -------------------- */

    override fun assignmentsFlow(): Flow<List<Assignment>> =
        db.assignmentDao().streamAll()

    override fun assignmentsByActivityFlow(activityId: String): Flow<List<Assignment>> =
        db.assignmentDao().streamByActivity(activityId)

    override fun assignmentsByUserFlow(userId: String): Flow<List<Assignment>> =
        db.assignmentDao().streamByUser(userId)

    override suspend fun getAssignments(): List<Assignment> =
        db.assignmentDao().getAllOnce()

    override suspend fun addAssignment(assignment: Assignment): Boolean =
        withContext(Dispatchers.IO) {
            db.assignmentDao().insert(assignment) > 0L
        }

    override suspend fun removeAssignment(assignmentId: String) {
        db.assignmentDao().deleteById(assignmentId)
    }

    override fun requiredCountForActivityFlow(activityId: String): Flow<Int> =
        roleRequirementsFlow(activityId) // this already calls your DAO: requirementsForActivityFlow(activityId)
            .map { reqs -> reqs.sumOf { it.requiredCount } }

    override fun userProfessionsFlow(userId: String): Flow<Set<Profession>> = db.userProfessionDao()
        .professionsForUserFlow(userId)
        .map { it.toSet() }

    override suspend fun replaceUserProfessions(
        userId: String,
        professions: Set<Profession>
    ) {
        db.withTransaction {
            db.userProfessionDao().deleteAllForUser(userId)
            if (professions.isNotEmpty()) {
                val rows = professions.map { UserProfession(userId = userId, profession = it) }
                db.userProfessionDao().insertAll(rows)
            }
        }

    }

    /* --------- Activity role requirements (ARR) --------- */

    override fun roleRequirementsFlow(activityId: String): Flow<List<ActivityRoleRequirement>> =
        db.activityRoleRequirementDao().requirementsForActivityFlow(activityId)

    override suspend fun requirementsForActivity(activityId: String): List<ActivityRoleRequirement> =
        db.activityRoleRequirementDao().requirementsForActivity(activityId)

    override suspend fun upsertAllRequirements(items: List<ActivityRoleRequirement>) {
        db.activityRoleRequirementDao().upsertAll(items)
    }

    override suspend fun upsertRequirement(item: ActivityRoleRequirement) {
        db.activityRoleRequirementDao().upsert(item)
    }

    override suspend fun deleteRequirement(activityId: String, profession: Profession) {
        db.activityRoleRequirementDao().deleteRequirement(activityId, profession)
    }

    override fun assignedCountsAllFlow() =
        db.assignmentDao().assignedCountsAllFlow()

    override fun requiredCountsAllFlow() =
        db.activityRoleRequirementDao().requiredCountsAllFlow()

    override suspend fun deleteAllRequirementsForActivity(activityId: String) {
        db.activityRoleRequirementDao().deleteAllForActivity(activityId)
    }

    override fun assignedCountForActivityFlow(activityId: String): Flow<Int> =
        assignmentsByActivityFlow(activityId)
            .map { list -> list.size }
}