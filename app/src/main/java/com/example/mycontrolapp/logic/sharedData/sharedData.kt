package com.example.mycontrolapp.logic.sharedData
import com.example.mycontrolapp.logic.sharedEnums.Profession

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

data class TimeSegment(
    val userId: String,
    val start: Long,
    val end: Long
)

data class TimeSplitUiState(
    val enabled: Boolean = false,
    val minutesInput: String = "",
    val segments: List<TimeSegment> = emptyList(),
    val showAssignments: Boolean = true
)




