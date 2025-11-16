package com.example.mycontrolapp.logic.sharedData
import android.os.Parcelable
import com.example.mycontrolapp.logic.sharedEnums.Profession
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

data class AssignedCountRow(
    val activityId: String,
    val assigned: Int
)

data class RequiredCountRow(
    val activityId: String,
    val required: Int
)

data class RoleNeed(
    val profession: Profession,
    val seatIndex: Int
)

data class AssignmentDraft(
    val userId: String,
    val profession: Profession
)

@IgnoreExtraProperties
@Parcelize
data class TimeSegment(
    var userId: String = "",
    var start: Long = 0L,
    var end: Long = 0L
) : Parcelable {
    constructor() : this("", 0L, 0L)
}

@Parcelize
data class TimeSplitUiState(
    val enabled: Boolean = false,
    val minutesInput: String = "",
    val segments: List<TimeSegment> = emptyList(),
    val showAssignments: Boolean = true
): Parcelable




