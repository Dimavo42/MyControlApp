package com.example.mycontrolapp.ui.componentes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mycontrolapp.logic.User
import java.time.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilterWithShow(
    users: List<User>,
    modifier: Modifier = Modifier,
    onShow: (YearMonth, User?) -> Unit = { _, _ -> },
    content: @Composable (displayedYearMonth: YearMonth, selectedUser: User?) -> Unit
) {
    val nowYm = YearMonth.now()

    // Month/Year menus
    var monthMenu by rememberSaveable { mutableStateOf(false) }
    var yearMenu by rememberSaveable { mutableStateOf(false) }

    // Month/Year selection
    var selectedMonth by rememberSaveable { mutableStateOf(nowYm.month) }
    var selectedYear by rememberSaveable { mutableStateOf(nowYm.year) }

    // Committed user selection (null = All users)
    var selectedUserId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedUser: User? = remember(users, selectedUserId) { users.firstOrNull { it.id == selectedUserId } }

    // Text shown/edited in the field (independent, only synced on commit or revert)
    val committedLabel = selectedUser?.name ?: "All users"
    var userQuery by rememberSaveable { mutableStateOf(committedLabel) }

    // Displayed calendar YM
    var calendarYm by rememberSaveable { mutableStateOf(nowYm) }

    val years = remember {
        val base = LocalDate.now().year
        (base - 5..base + 5).toList()
    }

    // Dropdown for full list (opens via chevron only)
    var userMenuExpanded by rememberSaveable { mutableStateOf(false) }

    // Matching helpers (no suggestions UI, only apply on Show)
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
            "Select the date for activities",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("lblSelectDate")
        )

        // ── Row 1: User filter (no suggestions; chevron opens full list) ──
        ExposedDropdownMenuBox(
            expanded = userMenuExpanded,
            onExpandedChange = { /* only chevron toggles */ },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("userFilterRow")
        ) {
            OutlinedTextField(
                value = userQuery,
                onValueChange = { text ->
                    // Let the user freely type/delete; don't auto-select here
                    userQuery = text
                },
                enabled = users.isNotEmpty(),
                label = { Text("Filter by user") },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = { if (users.isNotEmpty()) userMenuExpanded = !userMenuExpanded },
                        enabled = users.isNotEmpty()
                    ) {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = userMenuExpanded)
                    }
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
                // "All users"
                DropdownMenuItem(
                    text = { Text("All users") },
                    onClick = {
                        selectedUserId = null
                        userQuery = "All users" // commit label
                        userMenuExpanded = false
                    },
                    modifier = Modifier.testTag("user_all")
                )
                // Actual users
                users.forEach { u ->
                    DropdownMenuItem(
                        text = { Text(u.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                            selectedUserId = u.id
                            userQuery = u.name // commit label
                            userMenuExpanded = false
                        },
                        modifier = Modifier.testTag("user_${u.id}")
                    )
                }
            }
        }

        // ── Row 2: Month | Year | Show ──
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btnMonth")
                ) { Text(selectedMonth.name) }

                DropdownMenu(
                    expanded = monthMenu,
                    onDismissRequest = { monthMenu = false }
                ) {
                    Month.values().forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m.name) },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btnYear")
                ) { Text(selectedYear.toString()) }

                DropdownMenu(
                    expanded = yearMenu,
                    onDismissRequest = { yearMenu = false }
                ) {
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

            // Show (apply filters)
            Button(
                onClick = {
                    val q = userQuery.trim()
                    var effectiveUser: User? = selectedUser // default: keep current selection

                    when {
                        q.equals("all", true) || q.equals("all users", true) -> {
                            selectedUserId = null
                            userQuery = "All users"
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
                                // Not identifiable → revert the field back to the committed label
                                userQuery = committedLabel
                            }
                        }
                        else -> {
                            // Empty query → revert to committed label
                            userQuery = committedLabel
                        }
                    }

                    val ym = YearMonth.of(selectedYear, selectedMonth)
                    calendarYm = ym
                    onShow(ym, effectiveUser)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("btnShow")
            ) { Text("Show") }
        }

        // Render caller content with the effective YM and selected user
        content(calendarYm, selectedUser)
    }
}
