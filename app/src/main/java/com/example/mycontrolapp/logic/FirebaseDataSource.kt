package com.example.mycontrolapp.logic
import com.example.mycontrolapp.logic.sharedEnums.Profession
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class FirebaseDataSource @Inject constructor(
    private val fs: FirebaseFirestore
) {
    private val activities get() = fs.collection("activities")
    private val users get() = fs.collection("users")
    private val assignments get() = fs.collection("assignments")
    private val requirements get() = fs.collection("activity_role_requirements")
    private val userProfessions get() = fs.collection("user_professions")
    private val activityTimeSplit get() = fs.collection("activity_time_split")


    // ---------- Pull everything for one-shot sync ----------
    suspend fun pullAll(): RemoteSnapshot = RemoteSnapshot(
        activities = activities.get().await().toObjects(Activity::class.java),
        users = users.get().await().toObjects(User::class.java),
        assignments = assignments.get().await().toObjects(Assignment::class.java),
        requirements = requirements.get().await().toObjects(ActivityRoleRequirement::class.java),
        userProfessions = userProfessions.get().await().toObjects(UserProfession::class.java),
        activityTimeSplit = activityTimeSplit.get().await().toObjects(ActivityTimeSplit::class.java)
    )

    data class RemoteSnapshot(
        val activities: List<Activity>,
        val users: List<User>,
        val assignments: List<Assignment>,
        val requirements: List<ActivityRoleRequirement>,
        val userProfessions: List<UserProfession>,
        val activityTimeSplit:List<ActivityTimeSplit>
    )

    // ---------- Upserts / deletes to mirror Room mutations ----------
    suspend fun upsertActivity(a: Activity) {
        activities.document(a.id).set(a, SetOptions.merge()).await()
    }
    suspend fun deleteActivity(id: String) {
        activities.document(id).delete().await()
    }

    suspend fun upsertUser(u: User) {
        users.document(u.id).set(u, SetOptions.merge()).await()
    }
    suspend fun deleteUser(id: String) {
        users.document(id).delete().await()
    }

    suspend fun upsertAssignment(a: Assignment) {
        assignments.document(a.id).set(a, SetOptions.merge()).await()
    }
    suspend fun deleteAssignment(id: String) {
        assignments.document(id).delete().await()
    }

    suspend fun upsertRequirement(r: ActivityRoleRequirement) {
        // deterministic doc id per (activity, profession)
        val docId = "${r.activityId}_${r.profession.name}"
        requirements.document(docId).set(r, SetOptions.merge()).await()
    }

    suspend fun upsertAllRequirements(requirementsList: List<ActivityRoleRequirement>) {
        if (requirementsList.isEmpty()) return
        val batch = fs.batch()
        requirementsList.forEach { r ->
            val docId = "${r.activityId}_${r.profession.name}"
            val docRef = requirements.document(docId)
            batch.set(docRef, r, SetOptions.merge())
        }
        batch.commit().await()
    }


    suspend fun deleteRequirement(activityId: String, profession: Profession) {
        val docId = "${activityId}_${profession.name}"
        requirements.document(docId).delete().await()
    }
    suspend fun deleteAllRequirementsForActivity(activityId: String) {
        val snap = requirements.whereEqualTo("activityId", activityId).get().await()
        for (d in snap.documents) requirements.document(d.id).delete().await()
    }

    suspend fun replaceUserProfessions(userId: String, profs: Set<Profession>) {
        // delete existing rows for user
        val existing = userProfessions.whereEqualTo("userId", userId).get().await()
        for (d in existing.documents) userProfessions.document(d.id).delete().await()
        // insert new set
        profs.forEach { p ->
            val id = "${userId}_${p.name}"
            userProfessions.document(id).set(UserProfession(userId = userId, profession = p)).await()
        }
    }

    suspend fun replaceActivityTimeSplit(row: ActivityTimeSplit) {
        activityTimeSplit.document(row.activityId).set(row).await() // full replace
    }
    suspend fun deleteActivityTimeSplit(activityId: String) {
        activityTimeSplit.document(activityId).delete().await()
    }

    suspend fun replaceAssignmentsForActivity(
        activityId: String,
        assignmentsList: List<Assignment>
    ) {
        // 1) Load existing docs for this activity
        val existing = assignments
            .whereEqualTo("activityId", activityId)
            .get()
            .await()
        val batch = fs.batch()
        for (doc in existing.documents) {
            batch.delete(doc.reference)
        }
        assignmentsList.forEach { assignment ->
            val docId = assignment.id
            val docRef = assignments.document(docId)
            batch.set(docRef, assignment)
        }
        batch.commit().await()
    }
}