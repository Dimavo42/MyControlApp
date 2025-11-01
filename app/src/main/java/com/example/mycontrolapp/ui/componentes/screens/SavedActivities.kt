package com.example.mycontrolapp.ui.componentes.screens
import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mycontrolapp.ui.componentes.ActivityViewModel
import com.example.mycontrolapp.ui.componentes.DateFilterWithShow
import java.time.YearMonth
import com.example.mycontrolapp.R
import com.example.mycontrolapp.logic.sharedEnums.Team
import java.time.format.DateTimeFormatter
import java.util.Locale

@SuppressLint("LocalContextConfigurationRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedActivities(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    // Users for the filter dropdown
    val users by viewModel.usersFlow.collectAsState(initial = emptyList())

    // Filtered list (by month + user) from VM
    val awuFiltered by viewModel.activitiesWithUsersFilteredFlow
        .collectAsState(initial = emptyList())

    var selected by remember { mutableStateOf<ActivityViewModel.ActivityWithUsers?>(null) }
    val dialogOpen = selected != null

    Scaffold(
        topBar = {
            Text(
                text = stringResource(R.string.saved_activities_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { padding ->
        DateFilterWithShow(
            users = users,
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            onShow = { ym: YearMonth, userOrNull,selectedTeam ->
                viewModel.setSelectedYearMonth(ym)
                viewModel.setSelectedUser(userOrNull?.id) // null => All users
                viewModel.setSelectedTeam(selectedTeam ?: Team.Unknown )
            }
        ) { displayedYm, _ ->
            val context = LocalContext.current
            val locale = remember { context.resources.configuration.locales[0] ?: Locale.getDefault() }
            val monthYearLabel = remember(displayedYm, locale) {
                displayedYm.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
            }

            if (awuFiltered.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_activities_in_month, monthYearLabel))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = awuFiltered,
                        key = { it.activity.id }
                    ) { item ->
                        // Collect counts for each row
                        val assignedCount by remember(item.activity.id) {
                            viewModel.assignedCountForActivityFlow(item.activity.id)
                        }.collectAsState(initial = 0)

                        val requiredCount by remember(item.activity.id) {
                            viewModel.requiredCountForActivity(item.activity.id)
                        }.collectAsState(initial = 0)

                        ActivityRow(
                            item = item,
                            assignedCount = assignedCount,
                            requiredCount = requiredCount,
                            onClick = { selected = item },
                            onAssignOrEditClick = {
                                navController.navigate("assignment/${item.activity.id}")
                            },
                            onDeleteClick = {
                                viewModel.removeActivity(item.activity.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Details dialog
    if (dialogOpen) {
        val item = selected!!
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(item.activity.name) },
            text = {
                if (item.users.isEmpty()) {
                    Text(stringResource(R.string.no_users_assigned))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.assigned_users_label))
                        item.users.forEach { user ->
                            Text("• ${user.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selected = null }) {
                    Text(stringResource(R.string.common_close))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val id = item.activity.id
                        selected = null
                        navController.navigate("assignment/$id")
                    }
                ) { Text(stringResource(R.string.manage_button)) }
            }
        )
    }
}

@Composable
private fun ActivityRow(
    item: ActivityViewModel.ActivityWithUsers,
    assignedCount: Int,
    requiredCount: Int,
    onClick: () -> Unit,
    onAssignOrEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isFullyAssigned = requiredCount > 0 && assignedCount >= requiredCount

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = item.activity.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(6.dp))

            // Show "assigned/required" like 1/2, 0/10
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val counter = "$assignedCount/$requiredCount"
                val counterStyle = if (isFullyAssigned)
                    MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                else
                    MaterialTheme.typography.bodyMedium

                Text(text = counter, style = counterStyle)

                if (isFullyAssigned) {
                    AssistChip(
                        onClick = { /* info only */ },
                        label = { Text(stringResource(R.string.status_full)) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isFullyAssigned) {
                    Button(onClick = onAssignOrEditClick) {
                        Text(stringResource(R.string.action_edit))
                    }
                } else {
                    OutlinedButton(onClick = onAssignOrEditClick) {
                        Text(stringResource(R.string.action_assign_ellipsis))
                    }
                }
                OutlinedButton(onClick = onDeleteClick) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
    }
}
