package com.example.mycontrolapp.ui.componentes.custom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.mycontrolapp.logic.User
import com.example.mycontrolapp.logic.sharedEnums.Profession
import kotlin.collections.forEach
import com.example.mycontrolapp.R
import com.example.mycontrolapp.logic.sharedData.TimeSegment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentRow(
    profession: Profession,
    options: List<User>,
    selectedUserId: String?,
    onSelect: (User?) -> Unit,
    onAssign: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.id == selectedUserId }
    val hasOptions = options.isNotEmpty()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rowQuickAssign_${profession.name}")
            .semantics { contentDescription = "resourceId:rowQuickAssign_${profession.name}" },
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("${stringResource(profession.labelRes)} →", modifier = Modifier.weight(1f))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (hasOptions) expanded = !expanded },
            modifier = Modifier.weight(2f)
        ) {
            OutlinedTextField(
                readOnly = true,
                value = if (hasOptions) (selected?.name ?: "") else stringResource(R.string.no_available_users),
                onValueChange = {},
                label = { Text(stringResource(R.string.label_select_for_role, stringResource(profession.labelRes))) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                enabled = hasOptions,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { u ->
                    DropdownMenuItem(
                        text = { Text(u.name) },
                        onClick = {
                            onSelect(u)
                            expanded = false
                        }
                    )
                }
            }

        }
    }
    Row{
        Button(
            onClick = onAssign,
            enabled = enabled,
        ) { Text(stringResource(R.string.action_assign)) }
    }
}