package com.example.mycontrolapp.ui.componentes.custom
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import com.example.mycontrolapp.logic.sharedEnums.Profession
import com.example.mycontrolapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentRow(
    index: Int,
    selected: Profession,
    onSelected: (Profession) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember { Profession.entries.toList() }

    Row(modifier = modifier.fillMaxWidth()) {
        Text(stringResource(R.string.role_number, index), modifier = Modifier.weight(1f))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(2f)
        ) {
            OutlinedTextField(
                value = stringResource(selected.labelRes),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.label_profession)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { prof ->
                    DropdownMenuItem(
                        text = { Text(stringResource(prof.labelRes)) },
                        onClick = {
                            onSelected(prof)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}