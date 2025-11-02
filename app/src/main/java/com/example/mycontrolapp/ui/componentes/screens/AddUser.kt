package com.example.mycontrolapp.ui.componentes.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mycontrolapp.logic.User
import com.example.mycontrolapp.logic.sharedEnums.Profession
import com.example.mycontrolapp.ui.componentes.ActivityViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import com.example.mycontrolapp.R
import com.example.mycontrolapp.logic.sharedEnums.Team

private enum class UserEditorMode { Add, Edit }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUser(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    var mode by rememberSaveable { mutableStateOf(UserEditorMode.Add) }

    // Users for Edit mode
    val users by viewModel.usersFlow.collectAsState(initial = emptyList())

    var userMenuExpanded by remember { mutableStateOf(false) }
    var selectedUserId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedUser = remember(users, selectedUserId) { users.firstOrNull { it.id == selectedUserId } }

    // Form fields
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var preferences by rememberSaveable { mutableStateOf("") }
    var canFillAnyRole by rememberSaveable { mutableStateOf(false) }

    // Team fields
    val allTeams = remember { Team.entries.filter { it != Team.Unknown } }
    var teamMenuExpanded by remember { mutableStateOf(false) }
    var selectedTeamName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedTeam: Team? = remember(selectedTeamName) {
        selectedTeamName?.let { nm -> Team.entries.firstOrNull { it.name == nm } }
    }

    // Professions (multi)
    val allProfessions = remember { Profession.entries.filter { it != Profession.Unknown } }
    val allProfessionNames = remember(allProfessions) { allProfessions.map { it.name }.toSet() }
    var professionsMenuExpanded by remember { mutableStateOf(false) }
    var selectedProfNames by rememberSaveable { mutableStateOf(setOf<String>()) }
    val selectedProfessions: Set<Profession> = remember(selectedProfNames, allProfessions) {
        selectedProfNames.mapNotNull { nm -> allProfessions.firstOrNull { it.name == nm } }.toSet()
    }

    // Helpers
    fun selectAll() { selectedProfNames = allProfessionNames }
    fun clearAll() { selectedProfNames = emptySet() }
    fun toggleAnyRole(checked: Boolean) {
        canFillAnyRole = checked
        if (checked) selectAll() else clearAll()
    }
    fun toggleProfession(prof: Profession) {
        val name = prof.name
        val wasSelected = name in selectedProfNames
        if (canFillAnyRole) {
            canFillAnyRole = false
            val start = allProfessionNames.toMutableSet()
            if (wasSelected) start.remove(name) else start.add(name)
            selectedProfNames = start
        } else {
            val newSet = if (wasSelected) selectedProfNames - name else selectedProfNames + name
            selectedProfNames = newSet
            if (newSet.size == allProfessions.size) canFillAnyRole = true
        }
    }

    // Load DB professions when editing
    val extrasFlow: Flow<Set<Profession>> = remember(selectedUser?.id, mode) {
        if (mode == UserEditorMode.Edit && selectedUser != null) {
            viewModel.userProfessionsFlow(selectedUser.id).distinctUntilChanged()
        } else flowOf(emptySet())
    }
    val extrasFromDb by extrasFlow.collectAsState(initial = emptySet())

    // Init/Reset fields when switching modes or users
    LaunchedEffect(mode, selectedUser?.id) {
        if (mode == UserEditorMode.Edit && selectedUser != null) {
            val parts = selectedUser.name.trim().split(" ")
            firstName = parts.firstOrNull().orEmpty()
            lastName = parts.drop(1).joinToString(" ")
            preferences = selectedUser.skills.joinToString(", ")
            canFillAnyRole = selectedUser.canFillAnyRole
            selectedTeamName = selectedUser.team?.name
        } else if (mode == UserEditorMode.Add) {
            firstName = ""; lastName = ""; preferences = ""; canFillAnyRole = false
            selectedUserId = null
            selectedProfNames = emptySet()
            selectedTeamName = null
        }
    }

    // Reflect DB professions into UI for Edit; if "any role" in DB, show all selected
    LaunchedEffect(extrasFromDb, canFillAnyRole, mode, selectedUser?.id, allProfessions) {
        if (mode == UserEditorMode.Edit && selectedUser != null) {
            selectedProfNames = if (canFillAnyRole) allProfessionNames else extrasFromDb.map { it.name }.toSet()
        }
    }

    val isFormValid = firstName.isNotBlank() && lastName.isNotBlank()
    val fieldsEnabled = mode == UserEditorMode.Add || (mode == UserEditorMode.Edit && selectedUser != null)

    // Close menus if fields become disabled
    LaunchedEffect(fieldsEnabled) {
        if (!fieldsEnabled) {
            professionsMenuExpanded = false
            teamMenuExpanded = false
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                if (mode == UserEditorMode.Add) stringResource(R.string.add_user_title_add)
                else stringResource(R.string.add_user_title_edit),
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            val tabs = listOf(UserEditorMode.Add, UserEditorMode.Edit)
            val idx = tabs.indexOf(mode)
            TabRow(selectedTabIndex = idx) {
                tabs.forEachIndexed { i, m ->
                    Tab(
                        selected = i == idx,
                        onClick = { mode = m },
                        text = {
                            Text(
                                if (m == UserEditorMode.Add) stringResource(R.string.tab_add)
                                else stringResource(R.string.tab_edit)
                            )
                        }
                    )
                }
            }
        }

        // Edit mode: user selector
        if (mode == UserEditorMode.Edit) {
            item {
                ExposedDropdownMenuBox(
                    expanded = userMenuExpanded,
                    onExpandedChange = { userMenuExpanded = !userMenuExpanded }
                ) {
                    TextField(
                        value = selectedUser?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_select_user)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = userMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = userMenuExpanded,
                        onDismissRequest = { userMenuExpanded = false }
                    ) {
                        users.forEach { user ->
                            DropdownMenuItem(
                                text = { Text(user.name) },
                                onClick = {
                                    selectedUserId = user.id
                                    userMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text(stringResource(R.string.label_first_name)) },
                enabled = fieldsEnabled,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text(stringResource(R.string.label_last_name)) },
                enabled = fieldsEnabled,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = preferences,
                onValueChange = { preferences = it },
                label = { Text(stringResource(R.string.label_skills_prefs)) },
                enabled = fieldsEnabled,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            ExposedDropdownMenuBox(
                expanded = teamMenuExpanded,
                onExpandedChange = {
                    if (fieldsEnabled) teamMenuExpanded = !teamMenuExpanded
                }
            ) {
                val teamDisplay = selectedTeam?.name
                    ?: stringResource(R.string.none_label)
                TextField(
                    value = teamDisplay,
                    onValueChange = {},
                    readOnly = true,
                    enabled = fieldsEnabled,
                    label = { Text(stringResource(R.string.team_choose)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teamMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = teamMenuExpanded && fieldsEnabled,
                    onDismissRequest = { teamMenuExpanded = false }
                ) {
                    // "None" option clears team
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.none_label)) },
                        onClick = {
                            selectedTeamName = null
                            teamMenuExpanded = false
                        }
                    )
                    // Actual teams (excluding Unknown)
                    allTeams.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.name) },
                            onClick = {
                                selectedTeamName = t.name
                                teamMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
        // ------------------------------------------------------

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Checkbox(
                    checked = canFillAnyRole,
                    onCheckedChange = { checked -> toggleAnyRole(checked) },
                    enabled = fieldsEnabled
                )
                Text(stringResource(R.string.label_can_fill_any_role))
            }
        }

        item { Text(text = stringResource(R.string.label_professions_multi), style = MaterialTheme.typography.labelLarge) }

        item {
            val display = selectedProfessions
                .map { stringResource(it.labelRes) }
                .sorted()
                .joinToString(", ")
                .ifEmpty { stringResource(R.string.none_label) }

            val canOpenMenu = fieldsEnabled
            ExposedDropdownMenuBox(
                expanded = professionsMenuExpanded,
                onExpandedChange = { if (canOpenMenu) professionsMenuExpanded = !professionsMenuExpanded }
            ) {
                TextField(
                    value = display,
                    onValueChange = {},
                    readOnly = true,
                    enabled = fieldsEnabled,
                    label = { Text(stringResource(R.string.label_professions)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = professionsMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = professionsMenuExpanded && fieldsEnabled,
                    onDismissRequest = { professionsMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_select_all)) },
                        onClick = { selectAll() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_clear_all)) },
                        onClick = { clearAll(); canFillAnyRole = false }
                    )
                    Divider()
                    allProfessions.forEach { prof ->
                        val selected = prof.name in selectedProfNames
                        DropdownMenuItem(
                            text = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Checkbox(
                                        checked = selected,
                                        onCheckedChange = { _ -> toggleProfession(prof) },
                                        enabled = fieldsEnabled
                                    )
                                    Text(stringResource(prof.labelRes))
                                }
                            },
                            onClick = { if (fieldsEnabled) toggleProfession(prof) },
                            enabled = fieldsEnabled
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                when (mode) {
                    UserEditorMode.Add -> {
                        Button(
                            enabled = isFormValid,
                            onClick = {
                                val skills = preferences.split(',')
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                val newUser = User(
                                    name = "${firstName.trim()} ${lastName.trim()}",
                                    skills = skills,
                                    isActive = true,
                                    canFillAnyRole = canFillAnyRole,
                                    team = selectedTeam                     // ⬅️ save team
                                )
                                viewModel.insertUser(newUser)
                                val setToSave = if (canFillAnyRole) emptySet() else selectedProfessions
                                viewModel.replaceUserProfessions(newUser.id, setToSave)
                                navController.navigateUp()
                            }
                        ) { Text(stringResource(R.string.action_accept)) }
                    }
                    UserEditorMode.Edit -> {
                        Button(
                            enabled = fieldsEnabled && isFormValid && selectedUser != null,
                            onClick = {
                                val skills = preferences.split(',')
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                val current = selectedUser ?: return@Button
                                val updated = current.copy(
                                    name = "${firstName.trim()} ${lastName.trim()}",
                                    skills = skills,
                                    canFillAnyRole = canFillAnyRole,
                                    team = selectedTeam                  // ⬅️ save team
                                )
                                viewModel.updateUser(updated)
                                val setToSave = if (canFillAnyRole) emptySet() else selectedProfessions
                                viewModel.replaceUserProfessions(updated.id, setToSave)
                                navController.navigateUp()
                            }
                        ) { Text(stringResource(R.string.action_save_changes)) }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (mode == UserEditorMode.Edit) {
                        OutlinedButton(
                            enabled = selectedUser != null,
                            onClick = {
                                val current = selectedUser ?: return@OutlinedButton
                                viewModel.removeUser(current.id)
                                selectedUserId = null
                                firstName = ""
                                lastName = ""
                                preferences = ""
                                canFillAnyRole = false
                                selectedProfNames = emptySet()
                                selectedTeamName = null
                            }
                        ) { Text(stringResource(R.string.action_delete)) }
                    }
                    OutlinedButton(
                        onClick = {
                            firstName = ""
                            lastName = ""
                            preferences = ""
                            canFillAnyRole = false
                            selectedProfNames = emptySet()
                            selectedTeamName = null
                            if (mode == UserEditorMode.Edit) selectedUserId = null
                        }
                    ) { Text(stringResource(R.string.action_reset)) }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { navController.navigateUp() }) { Text(stringResource(R.string.common_back)) }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}


