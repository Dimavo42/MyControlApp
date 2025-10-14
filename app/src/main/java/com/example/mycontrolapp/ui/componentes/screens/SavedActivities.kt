package com.example.mycontrolapp.ui.componentes.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mycontrolapp.ui.componentes.ActivityViewModel
import com.example.mycontrolapp.ui.componentes.DateFilterWithShow
import java.time.LocalDate
import java.time.YearMonth

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
                "Saved Activities",
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
            onShow = { ym: YearMonth, userOrNull ->
                viewModel.setSelectedYearMonth(ym)
                viewModel.setSelectedUser(userOrNull?.id) // null => All users
            }
        ) { displayedYm, _ ->
            if (awuFiltered.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No activities in ${displayedYm.month} ${displayedYm.year}.")
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
                    Text("No users assigned to this activity.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Assigned users:")
                        item.users.forEach { user ->
                            Text("• ${user.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selected = null }) { Text("Close") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val id = item.activity.id
                        selected = null
                        navController.navigate("assignment/$id")
                    }
                ) { Text("Manage") }
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

                Text(
                    text = counter,
                    style = counterStyle
                )

                if (isFullyAssigned) {
                    AssistChip(
                        onClick = { /* no-op / purely informative */ },
                        label = { Text("Full") }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isFullyAssigned) {
                    // Fully assigned → show a filled button labeled "Edit"
                    Button(onClick = onAssignOrEditClick) {
                        Text("Edit")
                    }
                } else {
                    // Not full → show outlined "Assign…" button
                    OutlinedButton(onClick = onAssignOrEditClick) {
                        Text("Assign…")
                    }
                }
                OutlinedButton(onClick = onDeleteClick) {
                    Text("Delete")
                }
            }
        }
    }
}
