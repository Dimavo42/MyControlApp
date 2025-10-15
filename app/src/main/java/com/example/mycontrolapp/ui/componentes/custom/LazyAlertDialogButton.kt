package com.example.mycontrolapp.ui.componentes.custom
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mycontrolapp.R

@Composable
fun <T> LazyAlertDialogButton(
    title: String,
    items: List<T>,
    optionalButtons: List<@Composable () -> Unit> = emptyList()
) {
    val isValidList = items.isNotEmpty()
    var showDialog by remember { mutableStateOf(false) }

    // Button that opens the dialog
    Column {
        Button(
            enabled = isValidList,
            onClick = { showDialog = true },
            modifier = Modifier.testTag("btnOpenDialog_$title")
        ) {
            Text(title)
        }
    }

    if (showDialog) {
        AlertDialog(
            modifier = Modifier.testTag("dialog_$title"),
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    stringResource(R.string.dialog_select_item),
                    modifier = Modifier.testTag("dialogTitle_$title")
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .padding(8.dp)
                        .heightIn(max = 300.dp)
                        .testTag("dialogList_$title")
                ) {
                    items(items) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .testTag("item_${item.toString()}")
                        ) {
                            Text(text = item.toString())
                            optionalButtons.forEach { button ->
                                button()
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    modifier = Modifier.testTag("btnCloseDialog_$title"),
                    onClick = { showDialog = false }
                ) {
                    Text(stringResource(R.string.common_close))
                }
            }
        )
    }
}





