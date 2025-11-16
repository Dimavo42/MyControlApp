package com.example.mycontrolapp.logic
import androidx.annotation.Keep
import androidx.room.*
import com.example.mycontrolapp.logic.sharedData.TimeSegment
import com.example.mycontrolapp.logic.sharedEnums.Profession
import com.example.mycontrolapp.logic.sharedEnums.Team
import com.example.mycontrolapp.logic.sharedEnums.TimeSplitMode
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import java.time.LocalDate
import java.util.*

@IgnoreExtraProperties
@Entity(tableName = "users")
data class User(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var isActive: Boolean = true,
    var canFillAnyRole: Boolean = false,
    var team: Team? = null
) {
    constructor() : this("", "", true, false, null)

    override fun toString(): String {
        val skillsString = team?.name ?: "no team"
        return "User -> $name -> $skillsString"
    }
}

// Cross-ref
@IgnoreExtraProperties
@Entity(
    tableName = "user_professions",
    primaryKeys = ["userId", "profession"],
    foreignKeys = [ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId"), Index("profession")]
)
data class UserProfession(
    var userId: String = "",
    var profession: Profession = Profession.Unknown
) {
    constructor() : this("", Profession.Unknown)
}

// ---------------- Activities ----------------
@Keep
@IgnoreExtraProperties
@Entity(
    tableName = "activities",
    indices = [Index("start_at"), Index("end_at"), Index("date_epoch_day")]
)
data class Activity(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    @ColumnInfo(name = "start_at") var startAt: Long = 0L,
    @ColumnInfo(name = "end_at")   var endAt: Long = 0L,
    @ColumnInfo(name = "date_epoch_day") var dateEpochDay: Int = 0,
    @ColumnInfo(name = "team") var team: Team? = null,
    @ColumnInfo(name = "time_split_mode") var timeSplitMode: TimeSplitMode = TimeSplitMode.NONE
) {
    constructor() : this("", "", 0L, 0L, 0, null, TimeSplitMode.NONE)

    val localDate: LocalDate get() = LocalDate.ofEpochDay(dateEpochDay.toLong())

    override fun toString(): String = "Activity : $name  -> $localDate"
}

// ---------------- Assignments ----------------

@IgnoreExtraProperties
@Entity(
    tableName = "assignments",
    foreignKeys = [
        ForeignKey(entity = Activity::class, parentColumns = ["id"], childColumns = ["activityId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index(value = ["activityId", "orderInActivity"], unique = true),
        Index("activityId"),
        Index("userId")
    ]
)
data class Assignment(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var activityId: String = "",
    var userId: String = "",
    var role: Profession = Profession.Unknown,
    var orderInActivity: Int = 0,
    var allocatedMinutes: Int? = null
) {
    constructor() : this("", "", "", Profession.Unknown, 0, null)
}

// ---------------- Requirements ----------------

@IgnoreExtraProperties
@Entity(
    tableName = "activity_role_requirements",
    foreignKeys = [
        ForeignKey(entity = Activity::class, parentColumns = ["id"], childColumns = ["activityId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["activityId", "profession"], unique = true), Index("activityId")]
)
data class ActivityRoleRequirement(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var activityId: String = "",
    var profession: Profession = Profession.Unknown,
    var requiredCount: Int = 0
) {
    constructor() : this("", "", Profession.Unknown, 0)
}

// ---------------- Time split ----------------

@IgnoreExtraProperties
@Entity(
    tableName = "activity_time_split",
    foreignKeys = [
        ForeignKey(entity = Activity::class, parentColumns = ["id"], childColumns = ["activityId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("activityId")]
)
data class ActivityTimeSplit(
    @PrimaryKey var activityId: String = "",
    var splitMinutes: Int = 0,
    var segments: List<TimeSegment> = emptyList()
) {
    constructor() : this("", 0, emptyList())
}