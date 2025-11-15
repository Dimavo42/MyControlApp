package com.example.mycontrolapp.logic.sharedData
import com.example.mycontrolapp.logic.sharedEnums.Profession
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable

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

@IgnoreExtraProperties
data class TimeSegment(
    var userId: String = "",
    var start: Long = 0L,
    var end: Long = 0L
) {
    constructor() : this("", 0L, 0L)
}

data class TimeSplitUiState(
    val enabled: Boolean = false,
    val minutesInput: String = "",
    val segments: List<TimeSegment> = emptyList(),
    val showAssignments: Boolean = true
)




