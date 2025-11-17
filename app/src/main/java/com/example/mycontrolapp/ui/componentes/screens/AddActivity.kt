package com.example.mycontrolapp.ui.componentes.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.example.myapp.appNavigation.AppDestinations
import com.example.mycontrolapp.logic.Activity
import com.example.mycontrolapp.logic.User
import com.example.mycontrolapp.logic.sharedEnums.Profession
import com.example.mycontrolapp.ui.componentes.ActivityViewModel
import com.example.mycontrolapp.ui.componentes.custom.AssignmentRow
import com.example.mycontrolapp.ui.componentes.custom.CandidatesInput
import com.example.mycontrolapp.ui.componentes.custom.ComputedTimeWindow
import com.example.mycontrolapp.ui.componentes.custom.TimeWindowContainer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.mycontrolapp.R
import com.example.mycontrolapp.logic.sharedEnums.Team
import kotlinx.coroutines.launch
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.mycontrolapp.core.MAX_CANDIDATES
import com.example.mycontrolapp.core.MIN_CANDIDATES


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivity(
    navController: NavController,
    modifier: Modifier = Modifier,
    prefillEpochDay: Long? = null,
    viewModel: ActivityViewModel = hiltViewModel()
) {

    var activityName by remember { mutableStateOf(TextFieldValue("")) }
    var dateText by remember { mutableStateOf(TextFieldValue("")) }
    var startTimeText by remember { mutableStateOf(TextFieldValue("")) }
    var endTimeText by remember { mutableStateOf(TextFieldValue("")) }

    // Waiting flag for the animation / overlay
    var isSaving by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(prefillEpochDay) {
        if (prefillEpochDay != null && dateText.text.isBlank()) {
            val ld = LocalDate.ofEpochDay(prefillEpochDay)
            dateText = TextFieldValue(ld.format(DateTimeFormatter.ofPattern("dd/MM/uuuu")))
            if (startTimeText.text.isBlank()) startTimeText = TextFieldValue("09:00")
            if (endTimeText.text.isBlank()) endTimeText = TextFieldValue("10:00")
        }
    }

    var candidates by rememberSaveable { mutableIntStateOf(0) }

    var roles by rememberSaveable(candidates) {
        mutableStateOf(List(candidates) { Profession.Solider })
    }
    LaunchedEffect(candidates) {
        roles = when {
            candidates < roles.size -> roles.take(candidates)
            candidates > roles.size -> roles + List(candidates - roles.size) { Profession.Solider }
            else -> roles
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    var attachTeamToActivity by rememberSaveable { mutableStateOf(false) }

    var computed by remember {
        mutableStateOf(
            ComputedTimeWindow(
                isDateValid = false,
                isStartValid = false,
                isEndValid = false,
                endAfterStart = false,
                dateEpochDay = null,
                startAtMillis = null,
                endAtMillis = null
            )
        )
    }

    val teamSaver = Saver<Team, String>(
        save = { it.name },
        restore = { name -> Team.entries.firstOrNull { it.name == name } ?: Team.Unknown }
    )
    var selectedTeam by rememberSaveable(stateSaver = teamSaver) { mutableStateOf(Team.Unknown) }
    var teamMenuExpanded by rememberSaveable { mutableStateOf(false) }

    var selectedUsers by remember { mutableStateOf<Set<User>>(emptySet()) }
    val assignCountOk = selectedUsers.size <= candidates

    val formValid =
        activityName.text.isNotBlank() &&
                computed.isDateValid && computed.isStartValid && computed.isEndValid &&
                computed.endAfterStart && candidates > MIN_CANDIDATES && assignCountOk

    val teamToPersist: Team? =
        if (attachTeamToActivity && selectedTeam != Team.Unknown) selectedTeam else null

    val canSubmit = formValid && !isSaving

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.create_activity_title),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item {
                OutlinedTextField(
                    value = activityName,
                    onValueChange = { activityName = it },
                    label = { Text(stringResource(R.string.label_activity_name)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                TimeWindowContainer(
                    dateText = dateText,
                    onDateTextChange = { dateText = it },
                    startTimeText = startTimeText,
                    onStartTimeTextChange = { startTimeText = it },
                    endTimeText = endTimeText,
                    onEndTimeTextChange = { endTimeText = it },
                    onComputedChange = { computed = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.add_activity_attach_team)
                    )
                    Switch(
                        checked = attachTeamToActivity,
                        onCheckedChange = { attachTeamToActivity = it },
                    )
                }
                if (attachTeamToActivity) {
                    ExposedDropdownMenuBox(
                        expanded = teamMenuExpanded,
                        onExpandedChange = { teamMenuExpanded = !teamMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = stringResource(selectedTeam.labelRes),
                            label = { Text(stringResource(R.string.select_team)) },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = teamMenuExpanded
                                )
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("txtTeamFilter")
                        )
                        ExposedDropdownMenu(
                            expanded = teamMenuExpanded,
                            onDismissRequest = { teamMenuExpanded = false }
                        ) {
                            Team.entries.forEach { team ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(team.labelRes)) },
                                    onClick = {
                                        selectedTeam = team
                                        teamMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                CandidatesInput(
                    value = candidates,
                    onValueChange = { v -> candidates = v.coerceIn(MIN_CANDIDATES, MAX_CANDIDATES) },
                    min = MIN_CANDIDATES,
                    max = MAX_CANDIDATES,
                    modifier = Modifier.fillMaxWidth(),
                    testTagPrefix = "candidates"
                )
            }

            // One row per candidate
            items(count = candidates, key = { it }) { idx ->
                AssignmentRow(
                    index = idx + 1,
                    selected = roles[idx],
                    onSelected = { newRole ->
                        roles = roles.toMutableList().also { it[idx] = newRole }
                    }
                )
            }

            if (!assignCountOk) {
                item {
                    Text(
                        stringResource(
                            R.string.error_selected_users_exceeds,
                            selectedUsers.size,
                            candidates
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (formValid) {
                        Button(
                            enabled = canSubmit,
                            onClick = {
                                val newActivity = Activity(
                                    name = activityName.text.trim(),
                                    startAt = requireNotNull(computed.startAtMillis),
                                    endAt = requireNotNull(computed.endAtMillis),
                                    dateEpochDay = requireNotNull(computed.dateEpochDay),
                                    team = teamToPersist
                                )
                                lifecycleOwner.lifecycleScope.launch {
                                    isSaving = true
                                    try {
                                        viewModel.insertActivityWithRequirements(
                                            activity = newActivity,
                                            roles = roles
                                        )
                                        navController.navigate("${AppDestinations.Assignment}/${newActivity.id}")
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            }
                        ) {
                            Text(stringResource(R.string.calendar_assign))
                        }
                    }

                    Button(
                        enabled = canSubmit,
                        onClick = {
                            val newActivity = Activity(
                                name = activityName.text.trim(),
                                startAt = requireNotNull(computed.startAtMillis),
                                endAt = requireNotNull(computed.endAtMillis),
                                dateEpochDay = requireNotNull(computed.dateEpochDay),
                                team = teamToPersist
                            )
                            lifecycleOwner.lifecycleScope.launch {
                                isSaving = true
                                try {
                                    viewModel.insertActivityWithRequirements(
                                        activity = newActivity,
                                        roles = roles
                                    )
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.action_accept))
                    }

                    OutlinedButton(
                        enabled = !isSaving,
                        onClick = {
                            activityName = TextFieldValue("")
                            dateText = TextFieldValue("")
                            startTimeText = TextFieldValue("")
                            endTimeText = TextFieldValue("")
                            candidates = 0
                            selectedUsers = emptySet()
                            roles = emptyList()
                            selectedTeam = Team.Unknown
                            attachTeamToActivity = false
                        }
                    ) {
                        Text(stringResource(R.string.action_reset))
                    }
                }
            }
        }

        // Waiting overlay with animation
        if (isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Card(
                    modifier = Modifier.align(Alignment.Center),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .widthIn(min = 200.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Saving, please wait…")
                        // or use stringResource(R.string.please_wait)
                    }
                }
            }
        }
    }
}



