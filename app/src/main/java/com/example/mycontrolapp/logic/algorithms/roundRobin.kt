package com.example.mycontrolapp.logic.algorithms

import com.example.mycontrolapp.logic.User
import com.example.mycontrolapp.logic.sharedData.TimeSegment
import com.example.mycontrolapp.logic.sharedEnums.Team

fun buildTimeSplitAssignments(
    startTime: Long,       // millis
    endTime: Long,         // millis
    splitSizeMinutes: Int, // minutes per segment
    unassignedUsers: List<User>,
    team: Team?
): List<TimeSegment> {

    if (splitSizeMinutes <= 0) return emptyList()

    val splitSizeMillis = splitSizeMinutes * 60_000L
    val totalTime = endTime - startTime
    if (totalTime <= 0) return emptyList()

    val listOfSuggestedAssigned =
        if (team == null) unassignedUsers
        else unassignedUsers.filter { it.team == team }

    if (listOfSuggestedAssigned.isEmpty()) return emptyList()

    val result = mutableListOf<TimeSegment>()

    var remaining = totalTime
    var currentTime = startTime
    var index = 0
    val size = listOfSuggestedAssigned.size

    while (remaining > 0) {
        val user = listOfSuggestedAssigned[index]

        val chunk = minOf(splitSizeMillis, remaining)
        val segmentStart = currentTime
        val segmentEnd = currentTime + chunk

        result += TimeSegment(
            userId = user.id,
            start = segmentStart,
            end = segmentEnd
        )

        currentTime = segmentEnd
        remaining -= chunk
        index = (index + 1) % size
    }

    return result
}