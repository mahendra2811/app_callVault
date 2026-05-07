package com.callNest.app.ui.screen.calldetail.sections

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.callNest.app.R
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Two-step picker: first asks for a [LocalDate] via [DatePickerDialog], then
 * for a [LocalTime] via [TimePicker]. Calls [onPicked] once both are chosen.
 *
 * Used by [FollowUpSection] to schedule reminders. The host owns the
 * persistence side via `ScheduleFollowUpUseCase`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpDateTimeDialog(
    onDismiss: () -> Unit,
    onPicked: (date: LocalDate, time: LocalTime) -> Unit
) {
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }

    if (pickedDate == null) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        val ms = state.selectedDateMillis ?: return@TextButton
                        val tz = TimeZone.currentSystemDefault()
                        pickedDate = Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz).date
                    }
                ) { Text(stringResource(R.string.followups_picker_pick_time)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.tag_editor_cancel))
                }
            }
        ) {
            DatePicker(state = state, modifier = Modifier.padding(8.dp))
        }
    } else {
        val timeState = rememberTimePickerState(initialHour = 9, initialMinute = 0)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.followups_picker_pick_time)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    onPicked(pickedDate!!, LocalTime(timeState.hour, timeState.minute))
                }) { Text(stringResource(R.string.followups_picker_save)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.tag_editor_cancel))
                }
            }
        )
    }
}
