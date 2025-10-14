package com.example.mycontrolapp.logic
import androidx.room.withTransaction
import com.example.mycontrolapp.logic.sharedEnums.Profession
import com.example.mycontrolapp.utils.RemoteEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Lazy

@Singleton
class ListManagerHybrid @Inject constructor(
    private val room: ListManagerRoom,
    private val db: AppDb,
    private val remoteLazy: Lazy<FirebaseDataSource>,   // <-- lazy, not eager
    @RemoteEnabled private val remoteEnabled: Boolean
) : ListManager by room {

    /**
     * Pulls all data from Firebase and persists into Room atomically.
     * Times out after [timeoutMs] (default 10s). Returns true on success, false on timeout/failure.
     * Won't even construct Firebase on emulator/debug when remoteEnabled == false.
     */
    suspend fun syncOnceWithTimeout(timeoutMs: Long = 10_000): Boolean {
        if (!remoteEnabled) return false
        val remote = remoteLazy.get()
        return withTimeoutOrNull(timeoutMs) {
            val snap = remote.pullAll()
            withContext(Dispatchers.IO) {
                db.withTransaction {
                    // Activities
                    snap.activities.forEach { db.activityDao().upsert(it) }
                    // Users
                    snap.users.forEach { db.userDao().upsert(it) }

                    // Assignments (replace)
                    db.assignmentDao().deleteAll()               // ensure DAO has this
                    snap.assignments.forEach { db.assignmentDao().insert(it) }

                    // Requirements (replace)
                    db.activityRoleRequirementDao().deleteAll()  // ensure DAO has this
                    db.activityRoleRequirementDao().upsertAll(snap.requirements)

                    // User professions (replace)
                    db.userProfessionDao().deleteAll()           // ensure DAO has this
                    db.userProfessionDao().insertAll(snap.userProfessions)
                }
            }
            true
        } ?: false
    }

    /* ------------------- Write-through (Room first) ------------------- */

    override suspend fun addActivity(activity: Activity) {
        room.addActivity(activity)
        if (remoteEnabled) runCatching { remoteLazy.get().upsertActivity(activity) }
    }

    override suspend fun updateActivity(activity: Activity): Boolean {
        val ok = room.updateActivity(activity)
        if (ok && remoteEnabled) runCatching { remoteLazy.get().upsertActivity(activity) }
        return ok
    }

    override suspend fun removeActivity(id: String) {
        room.removeActivity(id)
        if (remoteEnabled) runCatching { remoteLazy.get().deleteActivity(id) }
    }

    override suspend fun addUser(user: User) {
        room.addUser(user)
        if (remoteEnabled) runCatching { remoteLazy.get().upsertUser(user) }
    }

    override suspend fun updateUser(user: User) {
        room.updateUser(user)
        if (remoteEnabled) runCatching { remoteLazy.get().upsertUser(user) }
    }

    override suspend fun removeUser(id: String) {
        room.removeUser(id)
        if (remoteEnabled) runCatching { remoteLazy.get().deleteUser(id) }
    }

    override suspend fun addAssignment(assignment: Assignment): Boolean {
        val ok = room.addAssignment(assignment)
        if (ok && remoteEnabled) runCatching { remoteLazy.get().upsertAssignment(assignment) }
        return ok
    }

    override suspend fun removeAssignment(assignmentId: String) {
        room.removeAssignment(assignmentId)
        if (remoteEnabled) runCatching { remoteLazy.get().deleteAssignment(assignmentId) }
    }

    override suspend fun upsertRequirement(item: ActivityRoleRequirement) {
        room.upsertRequirement(item)
        if (remoteEnabled) runCatching { remoteLazy.get().upsertRequirement(item) }
    }

    override suspend fun deleteRequirement(activityId: String, profession: Profession) {
        room.deleteRequirement(activityId, profession)
        if (remoteEnabled) runCatching { remoteLazy.get().deleteRequirement(activityId, profession) }
    }

    override suspend fun deleteAllRequirementsForActivity(activityId: String) {
        room.deleteAllRequirementsForActivity(activityId)
        if (remoteEnabled) runCatching { remoteLazy.get().deleteAllRequirementsForActivity(activityId) }
    }

    override suspend fun setUserProfessions(userId: String, professions: Set<Profession>) {
        room.setUserProfessions(userId, professions)
        if (remoteEnabled) runCatching { remoteLazy.get().replaceUserProfessions(userId, professions) }
    }
    override fun assignedCountsAllFlow() = room.assignedCountsAllFlow()
    override fun requiredCountsAllFlow() = room.requiredCountsAllFlow()

    override suspend fun replaceUserProfessions(userId: String, professions: Set<Profession>) {
        room.replaceUserProfessions(userId, professions)
        if (remoteEnabled) runCatching { remoteLazy.get().replaceUserProfessions(userId, professions) }


    }
}