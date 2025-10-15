package com.example.mycontrolapp.ui.componentes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mycontrolapp.logic.User
import java.time.*
import com.example.mycontrolapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilterWithShow(
    users: List<User>,
    modifier: Modifier = Modifier,
    onShow: (YearMonth, User?) -> Unit = { _, _ -> },
    content: @Composable (displayedYearMonth: YearMonth, selectedUser: User?) -> Unit
) {
    val nowYm = YearMonth.now()
    val ctx = LocalContext.current
    var monthMenu by rememberSaveable { mutableStateOf(false) }
    var yearMenu by rememberSaveable { mutableStateOf(false) }

    var selectedMonth by rememberSaveable { mutableStateOf(nowYm.month) }
    var selectedYear by rememberSaveable { mutableStateOf(nowYm.year) }

    var selectedUserId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedUser: User? = remember(users, selectedUserId) { users.firstOrNull { it.id == selectedUserId } }

    val committedLabel = selectedUser?.name ?: stringResource(R.string.filter_all_users)
    var userQuery by rememberSaveable { mutableStateOf(committedLabel) }

    var calendarYm by rememberSaveable { mutableStateOf(nowYm) }

    val years = remember {
        val base = LocalDate.now().year
        (base - 5..base + 5).toList()
    }

    var userMenuExpanded by rememberSaveable { mutableStateOf(false) }

    // Localized month labels
    val monthLabels = stringArrayResource(id = R.array.month_names)

    fun exactMatch(q: String): User? =
        users.firstOrNull { it.name.equals(q, ignoreCase = true) }

    fun singlePartialMatch(q: String): User? {
        val matches = users.filter { it.name.contains(q, ignoreCase = true) }
        return matches.singleOrNull()
    }

    Column(
        modifier
            .padding(16.dp)
            .testTag("DateFilterWithShow"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.date_filter_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("lblSelectDate")
        )

        ExposedDropdownMenuBox(
            expanded = userMenuExpanded,
            onExpandedChange = { /* chevron only */ },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("userFilterRow")
        ) {
            OutlinedTextField(
                value = userQuery,
                onValueChange = { userQuery = it },
                enabled = users.isNotEmpty(),
                label = { Text(stringResource(R.string.label_filter_by_user)) },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = { if (users.isNotEmpty()) userMenuExpanded = !userMenuExpanded },
                        enabled = users.isNotEmpty()
                    ) { ExposedDropdownMenuDefaults.TrailingIcon(expanded = userMenuExpanded) }
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .testTag("txtUserFilter")
            )

            ExposedDropdownMenu(
                expanded = userMenuExpanded,
                onDismissRequest = { userMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.filter_all_users)) },
                    onClick = {
                        selectedUserId = null
                        userQuery = ctx.getString(R.string.filter_all_users) // << getString in callback
                        userMenuExpanded = false
                    },
                    modifier = Modifier.testTag("user_all")
                )
                users.forEach { u ->
                    DropdownMenuItem(
                        text = { Text(u.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                            selectedUserId = u.id
                            userQuery = u.name
                            userMenuExpanded = false
                        },
                        modifier = Modifier.testTag("user_${u.id}")
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .testTag("TopFilterRow"),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Month
            Box(modifier = Modifier.weight(1.5f)) {
                Button(
                    onClick = { monthMenu = true },
                    modifier = Modifier.fillMaxWidth().testTag("btnMonth")
                ) { Text(monthLabels[selectedMonth.value - 1]) }

                DropdownMenu(expanded = monthMenu, onDismissRequest = { monthMenu = false }) {
                    Month.values().forEach { m ->
                        DropdownMenuItem(
                            text = { Text(monthLabels[m.value - 1]) },
                            onClick = {
                                selectedMonth = m
                                monthMenu = false
                            },
                            modifier = Modifier.testTag("month_${m.value}")
                        )
                    }
                }
            }

            // Year
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { yearMenu = true },
                    modifier = Modifier.fillMaxWidth().testTag("btnYear")
                ) { Text(selectedYear.toString()) }

                DropdownMenu(expanded = yearMenu, onDismissRequest = { yearMenu = false }) {
                    years.forEach { y ->
                        DropdownMenuItem(
                            text = { Text(y.toString()) },
                            onClick = {
                                selectedYear = y
                                yearMenu = false
                            },
                            modifier = Modifier.testTag("year_$y")
                        )
                    }
                }
            }

            // Show
            Button(
                onClick = {
                    val q = userQuery.trim()
                    var effectiveUser: User? = selectedUser

                    val kwAll = ctx.getString(R.string.keyword_all)             // << use ctx here
                    val kwAllUsers = ctx.getString(R.string.keyword_all_users)
                    val allUsers = ctx.getString(R.string.filter_all_users)

                    when {
                        q.equals(kwAll, true) || q.equals(kwAllUsers, true) || q.equals(allUsers, true) -> {
                            selectedUserId = null
                            userQuery = allUsers
                            effectiveUser = null
                        }
                        q.isNotEmpty() -> {
                            val exact = exactMatch(q)
                            val unique = exact ?: singlePartialMatch(q)
                            if (unique != null) {
                                selectedUserId = unique.id
                                userQuery = unique.name
                                effectiveUser = unique
                            } else {
                                userQuery = committedLabel
                            }
                        }
                        else -> userQuery = committedLabel
                    }

                    val ym = YearMonth.of(selectedYear, selectedMonth)
                    calendarYm = ym
                    onShow(ym, effectiveUser)
                },
                modifier = Modifier.weight(1f).testTag("btnShow")
            ) { Text(stringResource(R.string.action_show)) }
        }

        content(calendarYm, selectedUser)
    }
}
