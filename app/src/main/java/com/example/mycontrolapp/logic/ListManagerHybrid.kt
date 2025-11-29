package com.example.mycontrolapp.logic
import androidx.room.withTransaction
import com.example.mycontrolapp.logic.sharedData.TimeSegment
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
    private val remoteLazy: Lazy<FirebaseDataSource>,
    @param:RemoteEnabled private val remoteEnabled: Boolean
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
                    // Build sets first (from remote)
                    val users = snap.users
                    val activities = snap.activities

                    val validUserIds = users.map { it.id }.toSet()
                    val validActivityIds = activities.map { it.id }.toSet()

                    val validAssignments = snap.assignments.filter { asg ->
                        asg.activityId in validActivityIds && asg.userId in validUserIds
                    }
                    val validRequirements = snap.requirements.filter { req ->
                        req.activityId in validActivityIds
                    }
                    val validUserProfessions = snap.userProfessions.filter { up ->
                        up.userId in validUserIds
                    }
                    val validTimeSplits = snap.activityTimeSplit.filter { ts ->
                        ts.activityId in validActivityIds
                    }

                    // 1) DELETE CHILD TABLES FIRST
                    db.assignmentDao().deleteAll()
                    db.activityRoleRequirementDao().deleteAll()
                    db.userProfessionDao().deleteAll()
                    db.activityTimeSplitDao().deleteAll()

                    // 2) DELETE PARENT TABLES
                    db.userDao().deleteAll()
                    db.activityDao().deleteAll()

                    // 3) INSERT PARENTS
                    users.forEach { user -> db.userDao().upsert(user) }
                    activities.forEach { activity -> db.activityDao().upsert(activity) }

                    // 4) INSERT CHILDREN
                    if (validAssignments.isNotEmpty()) {
                        db.assignmentDao().insertAll(validAssignments)
                    }
                    if (validRequirements.isNotEmpty()) {
                        db.activityRoleRequirementDao().upsertAll(validRequirements)
                    }
                    if (validUserProfessions.isNotEmpty()) {
                        db.userProfessionDao().insertAll(validUserProfessions)
                    }
                    if (validTimeSplits.isNotEmpty()) {
                        db.activityTimeSplitDao().upsertAll(validTimeSplits)
                    }
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
        if (remoteEnabled) runCatching {
            val remote = remoteLazy.get()
            remote.deleteActivity(id)
            remote.deleteActivityTimeSplit(id)
        }
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

    override suspend fun saveTimeSplitState(activityId: String, segments: List<TimeSegment>, splitMinutes: Int) {
        val row = ActivityTimeSplit(
            activityId = activityId,
            splitMinutes = splitMinutes,
            segments = segments
        )
        room.db.withTransaction {
            room.db.activityTimeSplitDao().deleteByActivityId(activityId)
            room.db.activityTimeSplitDao().upsert(row)
        }
        if (remoteEnabled) runCatching { remoteLazy.get().replaceActivityTimeSplit(row) }
    }

    override suspend fun replaceAssignmentsForActivity(
        activityId: String,
        newAssignments: List<Assignment>
    ) {
        room.replaceAssignmentsForActivity(activityId, newAssignments)
        if (remoteEnabled) {
            remoteLazy.get().replaceAssignmentsForActivity(activityId, newAssignments)
        }
    }

    override suspend fun clearTimeSplitState(activityId: String) {
        room.db.activityTimeSplitDao().deleteByActivityId(activityId)
        if (remoteEnabled) runCatching { remoteLazy.get().deleteActivityTimeSplit(activityId) }
    }

    override suspend fun syncOnceIfRemoteEnabled(): Boolean = if (remoteEnabled) syncOnceWithTimeout() else false

    override suspend fun upsertAllRequirements(items: List<ActivityRoleRequirement>) {
        room.db.activityRoleRequirementDao().upsertAll(items)
        if (remoteEnabled  && items.isNotEmpty()) runCatching {remoteLazy.get().upsertAllRequirements(items)}
    }
}