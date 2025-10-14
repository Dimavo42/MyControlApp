package com.example.mycontrolapp.logic
import androidx.room.*
import com.example.mycontrolapp.logic.sharedEnums.Profession
import java.time.LocalDate
import java.util.*

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val skills: List<String> = emptyList(),
    val isActive: Boolean = true,
    val canFillAnyRole: Boolean = false
) {
    override fun toString(): String {
        val skillsString = if (skills.isEmpty()) "No skills" else skills.joinToString("|")
        return "User -> $name -> $skillsString"
    }
}

/**
 * Cross-reference table for many-to-many User ↔ Profession.
 * If [User.canFillAnyRole] is true you can ignore this row when filtering eligibility.
 */
@Entity(
    tableName = "user_professions",
    primaryKeys = ["userId", "profession"],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),           // index FK
        Index("profession")        // fast lookups by role
    ]
)
data class UserProfession(
    val userId: String,
    val profession: Profession
)

/* ==============
   Activities
   ============== */

@Entity(
    tableName = "activities",
    indices = [
        Index("start_at"),
        Index("end_at"),
        Index("date_epoch_day")
    ]
)
data class Activity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    @ColumnInfo(name = "start_at") val startAt: Long,
    @ColumnInfo(name = "end_at")   val endAt: Long,
    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Int
) {
    init { require(endAt >= startAt) { "endAt must be >= startAt" } }

    @Ignore val localDate: LocalDate = LocalDate.ofEpochDay(dateEpochDay.toLong())

    override fun toString(): String = "Activity : $name  -> $localDate"
}

/* ==============
   Assignments
   ============== */

@Entity(
    tableName = "assignments",
    foreignKeys = [
        ForeignKey(
            entity = Activity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["activityId", "userId"], unique = true), // one assignment per user per activity
        Index("activityId"), // index FKs explicitly to avoid warnings
        Index("userId")
    ]
)
data class Assignment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val activityId: String,
    val userId: String,
    val role: Profession
)

/* ============================
   Activity Role Requirements
   ============================ */

@Entity(
    tableName = "activity_role_requirements",
    foreignKeys = [
        ForeignKey(
            entity = Activity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["activityId", "profession"], unique = true), // one row per role per activity
        Index("activityId")
    ]
)
data class ActivityRoleRequirement(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val activityId: String,
    val profession: Profession,
    val requiredCount: Int
)