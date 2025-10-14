package com.example.mycontrolapp.ui.componentes.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mycontrolapp.logic.Activity
import com.example.mycontrolapp.logic.ActivityRoleRequirement
import com.example.mycontrolapp.logic.User
import com.example.mycontrolapp.logic.sharedEnums.Profession
import com.example.mycontrolapp.ui.componentes.ActivityViewModel
import com.example.mycontrolapp.ui.componentes.custom.AssignmentRow
import com.example.mycontrolapp.ui.componentes.custom.CandidatesInput
import com.example.mycontrolapp.ui.componentes.custom.ComputedTimeWindow
import com.example.mycontrolapp.ui.componentes.custom.TimeWindowContainer
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivity(
    navController: NavController,
    modifier: Modifier = Modifier,
    prefillEpochDay: Long? = null,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val minCandidates = 0
    val maxCandidates = 10

    var activityName by remember { mutableStateOf(TextFieldValue("")) }
    var dateText by remember { mutableStateOf(TextFieldValue("")) }
    var startTimeText by remember { mutableStateOf(TextFieldValue("")) }
    var endTimeText by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(prefillEpochDay) {
        if (prefillEpochDay != null && dateText.text.isBlank()) {
            val ld = LocalDate.ofEpochDay(prefillEpochDay)
            dateText = TextFieldValue(ld.format(DateTimeFormatter.ofPattern("dd/MM/uuuu")))
            if (startTimeText.text.isBlank()) startTimeText = TextFieldValue("09:00")
            if (endTimeText.text.isBlank()) endTimeText = TextFieldValue("10:00")
        }
    }

    var candidates by rememberSaveable { mutableIntStateOf(0) }

    // Keep roles sized to candidates
    var roles by rememberSaveable(candidates) {
        mutableStateOf(List(candidates) { Profession.Unknown })
    }
    LaunchedEffect(candidates) {
        roles = when {
            candidates < roles.size -> roles.take(candidates)
            candidates > roles.size -> roles + List(candidates - roles.size) { Profession.Unknown }
            else -> roles
        }
    }

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

    // You don't pick users here yet, but keep the logic intact
    var selectedUsers by remember { mutableStateOf<Set<User>>(emptySet()) }
    val assignCountOk = selectedUsers.size <= candidates

    val formValid =
        activityName.text.isNotBlank() &&
                computed.isDateValid && computed.isStartValid && computed.isEndValid &&
                computed.endAfterStart && candidates > minCandidates && assignCountOk

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("Create activity", style = MaterialTheme.typography.titleLarge)
        }

        item {
            OutlinedTextField(
                value = activityName,
                onValueChange = { activityName = it },
                label = { Text("Activity Name") },
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
            CandidatesInput(
                value = candidates,
                onValueChange = { v -> candidates = v.coerceIn(minCandidates, maxCandidates) },
                min = minCandidates,
                max = maxCandidates,
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
                    "You selected ${selectedUsers.size} users but candidates limit is $candidates.",
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
                Button(
                    enabled = formValid,
                    onClick = {
                        val newActivity = Activity(
                            name = activityName.text.trim(),
                            startAt = requireNotNull(computed.startAtMillis),
                            endAt = requireNotNull(computed.endAtMillis),
                            dateEpochDay = requireNotNull(computed.dateEpochDay)
                        )
                        viewModel.insertActivityWithRequirements(
                            activity = newActivity,
                            roles = roles
                        )

                        navController.navigateUp()
                    }
                ) { Text("Accept") }

                OutlinedButton(
                    onClick = {
                        activityName = TextFieldValue("")
                        dateText = TextFieldValue("")
                        startTimeText = TextFieldValue("")
                        endTimeText = TextFieldValue("")
                        candidates = 0
                        selectedUsers = emptySet()
                        roles = emptyList()
                    }
                ) { Text("Reset") }
            }
        }
    }
}



