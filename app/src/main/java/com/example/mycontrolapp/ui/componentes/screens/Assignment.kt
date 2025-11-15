package com.example.mycontrolapp.ui.componentes.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.appNavigation.AppDestinations
import com.example.mycontrolapp.logic.User
import com.example.mycontrolapp.logic.sharedData.RoleNeed
import com.example.mycontrolapp.logic.sharedEnums.Profession
import com.example.mycontrolapp.ui.componentes.ActivityViewModel
import com.example.mycontrolapp.ui.componentes.custom.AssignmentRow
import com.example.mycontrolapp.ui.componentes.custom.ComputedTimeWindow
import com.example.mycontrolapp.ui.componentes.custom.TimeWindowContainer
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.example.mycontrolapp.R
import com.example.mycontrolapp.logic.algorithms.buildTimeSplitAssignments
import com.example.mycontrolapp.logic.sharedEnums.Team
import com.example.mycontrolapp.logic.sharedEnums.UserEditorMode
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.mycontrolapp.logic.sharedData.TimeSplitUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentScreen(
    navController: NavController,
    activityId: String,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val activities by viewModel.activitiesFlow.collectAsState(initial = emptyList())
    val activity =
        remember(activities, activityId) { activities.firstOrNull { it.id == activityId } }

    if (activity == null) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.assignment_not_found),
                modifier = Modifier
                    .testTag("txtActivityNotFound")
                    .semantics { contentDescription = "resourceId:txtActivityNotFound" }
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .testTag("btnBackNotFound")
                    .semantics { contentDescription = "resourceId:btnBackNotFound" }
            ) { Text(stringResource(R.string.common_back)) }
        }
        return
    }

    // Users (to resolve names and build candidate lists)
    val users by viewModel.usersFlow.collectAsState(initial = emptyList())
    val usersById = remember(users) { users.associateBy { it.id } }

    // Unassigned users for this activity (used to filter per role)
    val unassignedUsers by remember(activityId) {
        viewModel.usersNotAssignedToActivity(activityId)
    }.collectAsState(initial = emptyList())

    // Assignments for THIS activity
    val assignmentsForActivity by remember(activityId) {
        viewModel.assignmentsFlow.map { list -> list.filter { it.activityId == activityId } }
    }.collectAsState(initial = emptyList())

    // REQUIRED roles
    val requirementsRaw by remember(activityId) {
        viewModel.roleRequirementsFlow(activityId)
    }.collectAsState(initial = emptyList())

    val requirements = remember(requirementsRaw) {
        requirementsRaw
            .filter { it.profession != Profession.Unknown }
            .filter { it.requiredCount > 0 }
    }

    /* --------------------- Activity edit state --------------------- */
    var mode by rememberSaveable { mutableStateOf(UserEditorMode.Fill) }

    var name by remember(activity.id) { mutableStateOf(activity.name) }
    var team by remember(activity.id) { mutableStateOf(activity.team) } // Team? (null = All users)
    var teamMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val zone = remember { ZoneId.systemDefault() }
    val initialLocalDate: LocalDate = LocalDate.ofEpochDay(activity.dateEpochDay.toLong())
    val initialStart: LocalTime = Instant.ofEpochMilli(activity.startAt).atZone(zone).toLocalTime()
    val initialEnd: LocalTime = Instant.ofEpochMilli(activity.endAt).atZone(zone).toLocalTime()

    fun LocalTime.toHhMm(): String = DateTimeFormatter.ofPattern("HH:mm").format(this)
    fun LocalDate.toDdMmYyyy(): String = DateTimeFormatter.ofPattern("dd/MM/uuuu").format(this)

    // Users that are already assigned to this activity (optionally filtered by team)
    val participantsForTimeSplit: List<User> = remember(assignmentsForActivity, usersById, team) {
        val assignedUsers = assignmentsForActivity
            .mapNotNull { asg -> usersById[asg.userId] }
            .distinctBy { it.id }   // just in case the same user appears multiple times

        if (team != null) {
            assignedUsers.filter { it.team == team }
        } else {
            assignedUsers
        }
    }

    var dateText by remember(activity.id) {
        mutableStateOf(
            TextFieldValue(
                initialLocalDate.toDdMmYyyy()
            )
        )
    }
    var startTimeText by remember(activity.id) {
        mutableStateOf(
            TextFieldValue(
                initialStart.toHhMm()
            )
        )
    }
    var endTimeText by remember(activity.id) {
        mutableStateOf(
            TextFieldValue(
                initialEnd.toHhMm()
            )
        )
    }

    var computed by remember(activity.id) {
        mutableStateOf(
            ComputedTimeWindow(
                isDateValid = true,
                isStartValid = true,
                isEndValid = true,
                endAfterStart = initialEnd.isAfter(initialStart),
                startAtMillis = activity.startAt,
                endAtMillis = activity.endAt,
                dateEpochDay = activity.dateEpochDay
            )
        )
    }

    val canSave =
        (mode == UserEditorMode.Edit) &&
                name.isNotBlank() &&
                computed.isDateValid &&
                computed.isStartValid &&
                computed.isEndValid &&
                computed.endAfterStart

    /* --------------------- Needed seats (requirements - assigned) --------------------- */

    // How many assigned per role right now?
    val assignedByRole: Map<Profession, Int> = remember(assignmentsForActivity) {
        assignmentsForActivity.groupBy { it.role }.mapValues { it.value.size }
    }

    // How many still needed per role?
    val neededSeats: List<RoleNeed> = remember(requirements, assignedByRole) {
        requirements.flatMap { req ->
            val done = assignedByRole[req.profession] ?: 0
            val remaining = (req.requiredCount - done).coerceAtLeast(0)
            List(remaining) { idx -> RoleNeed(req.profession, idx) }
        }
    }

    // All assignments are ready when there are no needed seats
    val allSeatsFilled = neededSeats.isEmpty()

    // Time split UI state (grouped)
    var timeSplitUiState by remember(activity.id) {
        mutableStateOf(TimeSplitUiState())
    }

    val savedTimeSplit by remember(activityId) {
        viewModel.timeSplitState(activityId)
    }.collectAsState(initial = null)

    // When DB state changes (first load / after save), sync into UI
    LaunchedEffect(savedTimeSplit) {
        savedTimeSplit?.let { state ->
            timeSplitUiState = timeSplitUiState.copy(
                enabled = state.segments.isNotEmpty(),
                minutesInput = state.splitMinutes.toString(),
                segments = state.segments
            )
        }
    }

    // If assignments are not full → disable the mode,
    // but DON'T delete the saved segments from DB.
    LaunchedEffect(allSeatsFilled) {
        if (!allSeatsFilled) {
            timeSplitUiState = timeSplitUiState.copy(enabled = false)
        }
    }

    fun optionsFor(): List<User> {
        return if (team == null) {
            unassignedUsers // open activity → all users
        } else {
            unassignedUsers.filter { it.team == team } // restricted to chosen team
        }
    }

    // For each "need" seat, keep a local selection until you click Assign
    var pending by remember(activity.id) { mutableStateOf<Map<String, String?>>(emptyMap()) }
    fun selectionKey(n: RoleNeed) = "${n.profession.name}#${n.seatIndex}"
    val zoneId = remember { ZoneId.systemDefault() }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    /* ---------------------------------- UI ---------------------------------- */
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("assignmentRoot")
            .semantics { contentDescription = "resourceId:assignmentRoot" },
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                stringResource(R.string.assignment_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .testTag("lblAssignmentTitle")
                    .semantics { contentDescription = "resourceId:lblAssignmentTitle" }
            )
        }

        // ---- Mode Tabs: Fill / Edit ----
        item {
            TabRow(
                selectedTabIndex = when (mode) {
                    UserEditorMode.Fill -> 0
                    else -> 1
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tabRowEditMode")
                    .semantics { contentDescription = "resourceId:tabRowEditMode" }
            ) {
                Tab(
                    selected = mode == UserEditorMode.Fill,
                    onClick = { mode = UserEditorMode.Fill },
                    modifier = Modifier
                        .testTag("tabFill")
                        .semantics { contentDescription = "resourceId:tabFill" },
                    text = { Text(stringResource(R.string.label_fill)) }
                )
                Tab(
                    selected = mode == UserEditorMode.Edit,
                    onClick = { mode = UserEditorMode.Edit },
                    modifier = Modifier
                        .testTag("tabEdit")
                        .semantics { contentDescription = "resourceId:tabEdit" },
                    text = { Text(stringResource(R.string.tab_edit)) }
                )
            }
        }

        // Activity name
        item {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.label_activity_name)) },
                enabled = (mode == UserEditorMode.Edit),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("txtActivityName")
                    .semantics { contentDescription = "resourceId:txtActivityName" }
            )
        }

        // Team: dropdown in Edit, read-only in Fill
        item {
            if (mode == UserEditorMode.Edit) {
                val valueLabel = team?.let { t ->
                    stringResource(t.labelRes)
                } ?: stringResource(R.string.keyword_all_users)

                ExposedDropdownMenuBox(
                    expanded = teamMenuExpanded,
                    onExpandedChange = { teamMenuExpanded = !teamMenuExpanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("boxActivityTeam")
                        .semantics { contentDescription = "resourceId:boxActivityTeam" }
                ) {
                    OutlinedTextField(
                        value = valueLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.assignment_label_team)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = teamMenuExpanded
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("txtActivityTeamEdit")
                            .semantics { contentDescription = "resourceId:txtActivityTeamEdit" }
                    )
                    DropdownMenu(
                        expanded = teamMenuExpanded,
                        onDismissRequest = { teamMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.keyword_all_users)) },
                            onClick = {
                                team = null
                                teamMenuExpanded = false
                            }
                        )
                        Team.entries
                            .filter { it != Team.Unknown }
                            .forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(t.labelRes)) },
                                    onClick = {
                                        team = t
                                        teamMenuExpanded = false
                                    }
                                )
                            }
                    }
                }
            } else {
                val teamText = team?.let { stringResource(it.labelRes) }
                    ?: stringResource(R.string.keyword_all_users)
                OutlinedTextField(
                    value = teamText,
                    onValueChange = {},
                    enabled = false,
                    readOnly = true,
                    label = { Text(stringResource(R.string.assignment_label_team)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("txtActivityTeam")
                        .semantics { contentDescription = "resourceId:txtActivityTeam" }
                )
            }
        }

        // Date/Time
        item {
            TimeWindowContainer(
                dateText = dateText,
                onDateTextChange = { dateText = it },
                startTimeText = startTimeText,
                onStartTimeTextChange = { startTimeText = it },
                endTimeText = endTimeText,
                onEndTimeTextChange = { endTimeText = it },
                enabled = (mode == UserEditorMode.Edit),
                showErrors = true,
                onComputedChange = { computed = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timeWindow")
                    .semantics { contentDescription = "resourceId:timeWindow" }
            )
        }

        // Save activity (only in Edit mode)
        if (mode == UserEditorMode.Edit) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        enabled = canSave,
                        onClick = {
                            val updated = activity.copy(
                                name = name.trim(),
                                startAt = computed.startAtMillis!!,
                                endAt = computed.endAtMillis!!,
                                dateEpochDay = computed.dateEpochDay!!,
                                team = team
                            )

                            val hasActivityChanged =
                                updated.startAt != activity.startAt ||
                                        updated.endAt != activity.endAt ||
                                        updated.dateEpochDay != activity.dateEpochDay ||
                                        updated.team != activity.team

                            if (hasActivityChanged && assignmentsForActivity.isNotEmpty()) {
                                assignmentsForActivity.forEach { asg ->
                                    viewModel.unassignUser(asg.activityId, asg.userId)
                                }
                                viewModel.clearTimeSplitState(activity.id)
                                timeSplitUiState = timeSplitUiState.copy(
                                    enabled = false,
                                    minutesInput = "",
                                    segments = emptyList()
                                )
                            }

                            viewModel.updateActivity(updated)
                            mode = UserEditorMode.Fill
                        },
                        modifier = Modifier
                            .testTag("btnSaveActivity")
                            .semantics { contentDescription = "resourceId:btnSaveActivity" }
                    ) { Text(stringResource(R.string.action_save_activity)) }
                }
            }
        }

        // ------------------ Needed assignments ------------------
        if (mode == UserEditorMode.Fill) {
            item {
                Text(
                    stringResource(R.string.title_needed_assignments),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .testTag("lblNeededAssignments")
                        .semantics { contentDescription = "resourceId:lblNeededAssignments" }
                )
            }

            // Time Split Mode switch
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = stringResource(R.string.assignment_time_split))
                    Switch(
                        checked = timeSplitUiState.enabled,
                        onCheckedChange = { checked ->
                            if (allSeatsFilled) {
                                timeSplitUiState = timeSplitUiState.copy(enabled = checked)
                            }
                        },
                        enabled = allSeatsFilled
                    )
                }
            }
            if (!allSeatsFilled) {
                item {
                    Text(
                        text = stringResource(R.string.assignment_label_TimeSplit_2),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            // Time Split controls
            if (timeSplitUiState.enabled) {
                val hasSavedState = savedTimeSplit?.segments?.isNotEmpty() == true

                item {
                    val enabledOptions = allSeatsFilled

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = timeSplitUiState.minutesInput,
                            onValueChange = { newValue ->
                                timeSplitUiState = timeSplitUiState.copy(
                                    minutesInput = newValue.filter { it.isDigit() }
                                )
                            },
                            enabled = enabledOptions,
                            label = { Text(stringResource(R.string.assignment_time_split)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // First "Apply" – only when we DON'T have saved state
                        Button(
                            onClick = {
                                val minutes = timeSplitUiState.minutesInput.toIntOrNull()
                                    ?: return@Button
                                val segments = buildTimeSplitAssignments(
                                    startTime = computed.startAtMillis!!,
                                    endTime = computed.endAtMillis!!,
                                    splitSizeMinutes = minutes,
                                    unassignedUsers = participantsForTimeSplit
                                )
                                timeSplitUiState = timeSplitUiState.copy(
                                    segments = segments
                                )
                                viewModel.saveTimeSplitState(
                                    activityId = activity.id,
                                    segments = segments,
                                    splitMinutes = minutes
                                )
                            },
                            enabled = enabledOptions &&
                                    timeSplitUiState.minutesInput.isNotBlank() &&
                                    !hasSavedState
                        ) {
                            Text(stringResource(R.string.action_apply))
                        }

                        // "Reapply" – only when there IS saved state
                        if (hasSavedState) {
                            OutlinedButton(
                                onClick = {
                                    // use current text if valid, otherwise fall back to saved minutes
                                    val minutes = timeSplitUiState.minutesInput.toIntOrNull()
                                        ?: savedTimeSplit?.splitMinutes
                                        ?: return@OutlinedButton

                                    val segments = buildTimeSplitAssignments(
                                        startTime = computed.startAtMillis!!,
                                        endTime = computed.endAtMillis!!,
                                        splitSizeMinutes = minutes,
                                        unassignedUsers = participantsForTimeSplit
                                    )

                                    timeSplitUiState = timeSplitUiState.copy(
                                        minutesInput = minutes.toString(),
                                        segments = segments
                                    )

                                    viewModel.saveTimeSplitState(
                                        activityId = activity.id,
                                        segments = segments,
                                        splitMinutes = minutes
                                    )
                                },
                                enabled = enabledOptions
                            ) {
                                Text(stringResource(R.string.action_reapply))
                            }
                        }
                    }
                }
            }

            if (neededSeats.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.msg_all_roles_full),
                        modifier = Modifier
                            .testTag("txtNoNeeds")
                            .semantics { contentDescription = "resourceId:txtNoNeeds" }
                    )
                }
            } else {
                items(
                    items = neededSeats,
                    key = { need -> "need_${need.profession.name}_${need.seatIndex}" }
                ) { need ->
                    val opts = optionsFor()
                    val hasOptions = opts.isNotEmpty()
                    val key = selectionKey(need)
                    val selectedId = pending[key]
                    AssignmentRow(
                        profession = need.profession,
                        options = opts,
                        selectedUserId = selectedId,
                        onSelect = { u ->
                            pending = pending.toMutableMap().apply { put(key, u?.id) }
                        },
                        onAssign = {
                            val uid = selectedId ?: return@AssignmentRow
                            viewModel.assignUser(
                                activityId = activity.id,
                                userId = uid,
                                profession = need.profession
                            )
                            // Clear selection for this slot; flows will update and this row will disappear
                            pending = pending.toMutableMap().apply { remove(key) }
                        },
                        enabled = hasOptions && selectedId != null
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 5.dp)
                    )
                }
            }

            // ------------------ Existing assignments ------------------
            item {
                Text(
                    stringResource(R.string.title_existing_assignments),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .testTag("lblExistingAssignments")
                        .semantics { contentDescription = "resourceId:lblExistingAssignments" }
                )
            }

            if (assignmentsForActivity.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.msg_no_assignments_yet),
                        modifier = Modifier
                            .testTag("txtNoAssignments")
                            .semantics { contentDescription = "resourceId:txtNoAssignments" }
                    )
                }
            } else {
                items(
                    items = assignmentsForActivity,
                    key = { it.id }
                ) { asg ->
                    val user = usersById[asg.userId]
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cardAssignment_${asg.id}")
                            .semantics {
                                contentDescription = "resourceId:cardAssignment_${asg.id}"
                            }
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    user?.name ?: stringResource(R.string.unknown_user)
                                )
                            },
                            supportingContent = { Text(stringResource(asg.role.labelRes)) },
                            trailingContent = {
                                TextButton(
                                    onClick = { viewModel.unassignUser(activityId, asg.userId) }
                                ) { Text(stringResource(R.string.action_unassign)) }
                            }
                        )
                    }
                }
            }

            // ------------------ Time split result cards ------------------
            if (timeSplitUiState.enabled && timeSplitUiState.segments.isNotEmpty()) {

                // Header + toggle
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Time Split Assignments",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.testTag("lblTimeSplitAssignments")
                        )

                        TextButton(
                            onClick = {
                                timeSplitUiState = timeSplitUiState.copy(
                                    showAssignments = !timeSplitUiState.showAssignments
                                )
                            },
                            modifier = Modifier.testTag("btnToggleTimeSplitAssignments")
                        ) {
                            Text(
                                if (timeSplitUiState.showAssignments)
                                    "Hide"
                                else
                                    "Show"
                            )
                        }
                    }
                }
                // Only show the cards when toggle is ON
                if (timeSplitUiState.showAssignments) {
                    items(
                        items = timeSplitUiState.segments,
                        key = { seg -> seg.hashCode() }
                    ) { seg ->
                        val start = Instant.ofEpochMilli(seg.start)
                            .atZone(zoneId)
                            .toLocalTime()
                        val end = Instant.ofEpochMilli(seg.end)
                            .atZone(zoneId)
                            .toLocalTime()
                        val userName = usersById[seg.userId]?.name ?: "Unassigned"

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("cardTimeSegment_${seg.hashCode()}")
                        ) {
                            ListItem(
                                headlineContent = { Text(userName) },
                                supportingContent = {
                                    Text(
                                        "${start.format(timeFormatter)} - ${
                                            end.format(timeFormatter)
                                        }"
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // ------------------ Bottom actions ------------------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        enabled = allSeatsFilled,
                        onClick = { navController.navigate(AppDestinations.Home) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .testTag("btnBack")
                    ) { Text("Save") }
                    if (allSeatsFilled) {
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .testTag("btnBack")
                                .semantics { contentDescription = "resourceId:btnBack" }
                        ) { Text(stringResource(R.string.common_back)) }
                    } else {
                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .testTag("btnBack")
                                .semantics { contentDescription = "resourceId:btnBack" }
                        ) { Text(stringResource(R.string.common_back)) }
                    }

                    OutlinedButton(
                        onClick = {
                            timeSplitUiState = TimeSplitUiState()
                            viewModel.clearTimeSplitState(activity.id)
                        },
                        modifier = Modifier
                            .testTag("btnResetTimeSplit")
                    ) {
                        Text("Reset Time Split")
                    }
                }
            }
        }
    }
}







