package com.example.loglife.ui.logging

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.util.DateTimeUtils

@Composable
fun EventEditDialog(
    event: EventInstanceWithType,
    onSaveClick: (EventInstance) -> Unit,
    onCancelClick: () -> Unit
) {
    var note by remember { mutableStateOf(event.eventInstance.note ?: "") }

    AlertDialog(
        onDismissRequest = onCancelClick,
        title = { Text("Edit ${event.eventType.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Start: ${getTimeString(event.eventInstance.startTime)}")
                val endTime = event.eventInstance.endTime
                if (endTime != null) {
                    Text("End: ${getTimeString(endTime)}")
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSaveClick(event.eventInstance.copy(note = note))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelClick) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NoteEditDialog(
    event: EventInstanceWithType,
    onSaveClick: (String) -> Unit,
    onCancelClick: () -> Unit
) {
    var note by remember { mutableStateOf(event.eventInstance.note ?: "") }

    AlertDialog(
        onDismissRequest = onCancelClick,
        title = { Text("Edit Note for ${event.eventType.name}") },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
            )
        },
        confirmButton = {
            Button(onClick = { onSaveClick(note) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelClick) {
                Text("Cancel")
            }
        }
    )
}

private fun getTimeString(timestampUtcMillis: Long?): String {
    return timestampUtcMillis?.let { DateTimeUtils.formatTime(it) } ?: "Unknown time"
}