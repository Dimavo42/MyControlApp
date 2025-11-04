package com.example.mycontrolapp.logic.algorithms

import com.example.mycontrolapp.logic.User
import com.example.mycontrolapp.logic.sharedData.TimeSegment
import com.example.mycontrolapp.logic.sharedEnums.Team

fun buildTimeSplitAssignments(
    startTimeText: String,
    endTimeText: String,
    timeSplitText: String,
    unassignedUsers: List<User>,
    team: Team?
): List<TimeSegment> {

    val startTime = startTimeText.toLong()
    val endTime = endTimeText.toLong()
    val splitSize = timeSplitText.toLong()

    val totalTime = endTime - startTime
    if (totalTime <= 0 || splitSize <= 0) return emptyList()

    val listOfSuggestedAssigned: List<User> =
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

        // last chunk can be smaller so we don't go past endTime
        val chunk = minOf(splitSize, remaining)
        val segmentStart = currentTime
        val segmentEnd = currentTime + chunk

        result += TimeSegment(
            userId = user.id,
            start = segmentStart,
            end = segmentEnd
        )

        currentTime = segmentEnd
        remaining -= chunk

        // round-robin: go to next user, wrap to 0 when at end
        index = (index + 1) % size
    }

    return result
}