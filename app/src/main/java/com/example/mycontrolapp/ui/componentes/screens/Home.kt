package com.example.mycontrolapp.ui.componentes.screens
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mycontrolapp.logic.Activity
import com.example.mycontrolapp.logic.User
import com.example.mycontrolapp.ui.componentes.ActivityViewModel
import com.example.mycontrolapp.ui.componentes.DateFilterWithShow
import com.example.mycontrolapp.ui.componentes.custom.CalendarView
import com.example.mycontrolapp.ui.componentes.custom.LazyAlertDialogButton
import java.time.YearMonth



@Composable
fun HomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    // Source data
    val users: List<User> by viewModel.usersFlow.collectAsState(initial = emptyList())

    // Filtered activities as list (for the Activities dialog)
    val awuFiltered by viewModel.activitiesWithUsersFilteredFlow.collectAsState(initial = emptyList())
    val activitiesFiltered: List<Activity> = remember(awuFiltered) { awuFiltered.map { it.activity } }

    // Filtered per-day map (for the calendar)
    val eventsByDateFiltered by viewModel.activitiesByDateFilteredFlow.collectAsState(initial = emptyMap())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top: date + user filter + calendar
        DateFilterWithShow(
            users = users,
            onShow = { ym: YearMonth, userOrNull ->
                viewModel.setSelectedYearMonth(ym)
                viewModel.setSelectedUser(userOrNull?.id) // null = all users
            }
        ) { displayedYm, _ ->
            CalendarView(
                navController = navController,
                yearMonth = displayedYm,
                eventsByDate = eventsByDateFiltered,              // filtered calendar data
                onDeleteActivity = { actId -> viewModel.removeActivity(actId) },
                modifier = Modifier.fillMaxWidth(),
                viewModel = viewModel                             // pass same VM (uses activityCountersFlow inside)
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LazyAlertDialogButton(title = "Users", items = users)
            LazyAlertDialogButton(title = "Activities", items = activitiesFiltered)
        }
    }
}

