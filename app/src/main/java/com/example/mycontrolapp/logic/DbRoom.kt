package com.example.mycontrolapp.logic
import androidx.room.*
import com.example.mycontrolapp.logic.sharedData.TimeSegment
import com.example.mycontrolapp.logic.sharedEnums.Profession
import com.example.mycontrolapp.logic.sharedEnums.Team
import com.example.mycontrolapp.logic.sharedEnums.TimeSplitMode
import java.time.LocalDate
import java.util.*

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isActive: Boolean = true,
    val canFillAnyRole: Boolean = false,
    val team: Team? = null
) {
    override fun toString(): String {
        val skillsString = team?.name?: "no team"
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
        Index("userId"),
        Index("profession")
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
    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Int,
    @ColumnInfo(name = "team") val team: Team? = null,
    @ColumnInfo(name = "time_split_mode") val timeSplitMode: TimeSplitMode = TimeSplitMode.NONE
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
        Index(value = ["activityId", "orderInActivity"], unique = true),
        Index("activityId"),
        Index("userId")
    ]
)
data class Assignment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val activityId: String,
    val userId: String,
    val role: Profession,
    val orderInActivity: Int = 0,
    val allocatedMinutes: Int? = null
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
        Index(value = ["activityId", "profession"], unique = true),
        Index("activityId")
    ]
)
data class ActivityRoleRequirement(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val activityId: String,
    val profession: Profession,
    val requiredCount: Int
)

@Entity(
    tableName = "activity_time_split",
    foreignKeys = [
        ForeignKey(
            entity = Activity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("activityId")
    ]
)
data class ActivityTimeSplit(
    @PrimaryKey val activityId: String,
    val splitMinutes: Int,
    val segments: List<TimeSegment>
)