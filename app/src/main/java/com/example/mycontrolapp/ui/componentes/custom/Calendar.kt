package com.example.mycontrolapp.ui.componentes.custom
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.myapp.appNavigation.AppDestinations
import com.example.mycontrolapp.logic.Activity
import com.example.mycontrolapp.ui.componentes.ActivityViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.mycontrolapp.R


@Composable
fun CalendarView(
    navController: NavController,
    yearMonth: YearMonth,
    eventsByDate: Map<LocalDate, List<Activity>>,   // filtered activities by date
    onDeleteActivity: (String) -> Unit,              // delete callback
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    // counters for each activity
    val countsByActivityId by viewModel.activityCountersFlow.collectAsState(initial = emptyMap())

    val dayLabels = stringArrayResource(id = R.array.calendar_weekdays).toList()


    // Month math
    val firstOfMonth = remember(yearMonth) { LocalDate.of(yearMonth.year, yearMonth.month, 1) }
    // Make Sunday = 0, Monday = 1, ... Saturday = 6
    val startOffset = remember(yearMonth) { firstOfMonth.dayOfWeek.value % 7 }
    val daysInMonth = remember(yearMonth) { yearMonth.lengthOfMonth() }

    val cells: List<LocalDate?> = remember(yearMonth) {
        buildList<LocalDate?>(42) {
            repeat(startOffset) { add(null) }
            for (d in 1..daysInMonth) add(LocalDate.of(yearMonth.year, yearMonth.month, d))
            while (size < 42) add(null)
        }
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .testTag("calendarRoot")
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .testTag("calendarHeader"),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calendarHeader_$label"),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 6 weeks x 7 days grid
        for (week in 0 until 6) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .testTag("calendarRow_week${week + 1}")
            ) {
                for (col in 0 until 7) {
                    val cellIndex = week * 7 + col
                    val date = cells[cellIndex]
                    if (date == null) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("calendarEmptyCell_${week}_${col}")
                        )
                    } else {
                        val activitiesOnDate = eventsByDate[date].orEmpty()
                        val hasActivities = activitiesOnDate.isNotEmpty()

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clickable {
                                    selectedDate = date
                                    showDialog = true
                                }
                                .background(
                                    if (hasActivities) Color(0xFFCF5F5F) else Color(0xFFAAD4A5),
                                    shape = MaterialTheme.shapes.small
                                )
                                .testTag("calendarDay_${date}_${if (hasActivities) "busy" else "free"}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                color = if (hasActivities) Color.White else Color.Black
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog with activities on selected date (filtered)
    if (showDialog && selectedDate != null) {
        val date = selectedDate!!
        val activitiesForSelected = eventsByDate[date].orEmpty()

        // Localized date formatting (system locale)
        val dateLabel = remember(date) {
            date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
        }

        AlertDialog(
            modifier = Modifier.testTag("dialog_activities_${date}"),
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    stringResource(R.string.calendar_activities_on, dateLabel),
                    modifier = Modifier.testTag("dialogTitle_${date}")
                )
            },
            text = {
                if (activitiesForSelected.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.testTag("dialogList_${date}")
                    ) {
                        items(activitiesForSelected, key = { it.id }) { activity ->
                            val assigned = countsByActivityId[activity.id]?.assigned ?: 0
                            val required = countsByActivityId[activity.id]?.required ?: 0

                            ActivityRowInDialog(
                                activity = activity,
                                assignedCount = assigned,
                                requiredCount = required,
                                onPrimaryClick = {
                                    // Primary is either Assign or Edit, both navigate to Assignment screen
                                    showDialog = false
                                    navController.navigate("${AppDestinations.Assignment}/${activity.id}")
                                },
                                onDelete = { onDeleteActivity(activity.id) }
                            )

                            Divider(
                                Modifier
                                    .padding(vertical = 8.dp)
                                    .testTag("divider_${activity.id}")
                            )
                        }
                    }
                } else {
                    // No activities on this date
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.calendar_no_activities),
                            modifier = Modifier.testTag("dialogEmpty_${date}")
                        )
                        Button(
                            modifier = Modifier.testTag("btnAssignEmpty_${date}"),
                            onClick = {
                                val epochDay = date.toEpochDay()
                                showDialog = false
                                // Navigate to Add Activity prefilled with this date
                                navController.popBackStack()
                                navController.navigate("${AppDestinations.ActivityWithDate}/$epochDay")
                            }
                        ) {
                            Text(stringResource(R.string.calendar_assign))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("btnCloseDialog_${date}"),
                    onClick = { showDialog = false }
                ) { Text(stringResource(R.string.calendar_close)) }
            }
        )
    }
}

@Composable
private fun ActivityRowInDialog(
    activity: Activity,
    assignedCount: Int,
    requiredCount: Int,
    onPrimaryClick: () -> Unit,
    onDelete: () -> Unit
) {
    // Full when there is a requirement and assigned >= required; if required=0 treat as full → show Edit
    val isFull = if (requiredCount > 0) assignedCount >= requiredCount else true
    val primaryLabelRes = if (isFull) R.string.calendar_edit else R.string.calendar_assign

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("activityRow_${activity.id}")
            .semantics { contentDescription = "resourceId:activityRow_${activity.id}" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // mission name -> current/required
        Text(
            text = "${activity.name} -> $assignedCount/$requiredCount",
            modifier = Modifier
                .padding(4.dp)
                .weight(1f)
                .testTag("activityText_${activity.id}")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.testTag("btnPrimary_${activity.id}"),
                onClick = onPrimaryClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(stringResource(primaryLabelRes), fontSize = 12.sp)
            }

            Button(
                modifier = Modifier.testTag("btnDelete_${activity.id}"),
                onClick = onDelete,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(stringResource(R.string.calendar_delete), fontSize = 12.sp)
            }
        }
    }
}
