package com.example.mycontrolapp.ui.componentes.custom
import android.app.DatePickerDialog
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale
import com.example.mycontrolapp.R

data class ComputedTimeWindow(
    val isDateValid: Boolean,
    val isStartValid: Boolean,
    val isEndValid: Boolean,
    val endAfterStart: Boolean,
    val startAtMillis: Long?,  // UTC epoch millis
    val endAtMillis: Long?,    // UTC epoch millis
    val dateEpochDay: Int?     // LocalDate.toEpochDay().toInt()
)

/**
 * A reusable container that renders:
 *  - a Date field with a DatePickerDialog
 *  - Start time (HH:mm) text field
 *  - End time (HH:mm) text field
 *
 * It validates inputs and computes start/end milliseconds and the date epoch day,
 * emitting them through [onComputedChange] whenever the inputs change.
 *
 * State is **hoisted**: you pass/own the three TextFieldValue states.
 */
@Composable
fun TimeWindowContainer(
    dateText: TextFieldValue,
    onDateTextChange: (TextFieldValue) -> Unit,
    startTimeText: TextFieldValue,
    onStartTimeTextChange: (TextFieldValue) -> Unit,
    endTimeText: TextFieldValue,
    onEndTimeTextChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showErrors: Boolean = true,
    zone: ZoneId = ZoneId.systemDefault(),
    allowOvernight: Boolean = false, // set true to allow 22:00 -> 01:00 (next day)
    onComputedChange: (ComputedTimeWindow) -> Unit = {}
) {
    val ctx = LocalContext.current

    // Preselect the native DatePicker based on current dateText or today
    val (preYear, preMonthZeroBased, preDay) = remember(dateText.text) {
        parseDdMmYyyyOrNull(dateText.text)?.let { cal ->
            Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        } ?: Calendar.getInstance().let { now ->
            Triple(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
        }
    }

    // Parse inputs
    val parsedDate: LocalDate? = remember(dateText.text) {
        parseDdMmYyyyToLocalDateOrNull(dateText.text)
    }
    val parsedStart: LocalTime? = remember(startTimeText.text) {
        parseHhMmOrNull(startTimeText.text)
    }
    val parsedEnd: LocalTime? = remember(endTimeText.text) {
        parseHhMmOrNull(endTimeText.text)
    }

    // Compute epoch millis and date epochDay if everything is present
    val startAt: Long? = remember(parsedDate, parsedStart, zone) {
        if (parsedDate != null && parsedStart != null)
            ZonedDateTime.of(parsedDate, parsedStart, zone).toInstant().toEpochMilli()
        else null
    }
    val endAt: Long? = remember(parsedDate, parsedEnd, zone) {
        if (parsedDate != null && parsedEnd != null)
            ZonedDateTime.of(parsedDate, parsedEnd, zone).toInstant().toEpochMilli()
        else null
    }

    // Time-only comparison (used when date is not set/valid).
    val endAfterStartTimeOnly = remember(parsedStart, parsedEnd, allowOvernight) {
        if (parsedStart == null || parsedEnd == null) {
            false
        } else if (allowOvernight) {
            // Example: 22:00 -> 01:00 should be considered valid
            parsedEnd.isAfter(parsedStart) || parsedEnd == parsedStart
                    || (!parsedEnd.isAfter(parsedStart)) // will be valid because it's "next day"
        } else {
            parsedEnd.isAfter(parsedStart)
        }
    }

    // Final validity:
    // - If date is missing, rely on time-only comparison.
    // - If date exists, compare millis (and optionally allow overnight by adding 1 day to end if needed).
    val endAfterStart = remember(parsedDate, parsedStart, parsedEnd, startAt, endAt, allowOvernight) {
        when {
            parsedStart == null || parsedEnd == null -> false
            parsedDate == null -> endAfterStartTimeOnly
            else -> {
                if (startAt == null || endAt == null) {
                    false
                } else {
                    val s = startAt
                    var e = endAt
                    if (allowOvernight && e <= s) {
                        e += Duration.ofDays(1).toMillis()
                    }
                    e > s
                }
            }
        }
    }

    val dateEpochDay = remember(parsedDate) { parsedDate?.toEpochDay()?.toInt() }

    // Emit computed result upward whenever inputs change
    LaunchedEffect(parsedDate, parsedStart, parsedEnd, startAt, endAt, endAfterStart, dateEpochDay) {
        onComputedChange(
            ComputedTimeWindow(
                isDateValid = parsedDate != null,
                isStartValid = parsedStart != null,
                isEndValid = parsedEnd != null,
                endAfterStart = endAfterStart,
                startAtMillis = startAt,
                endAtMillis = endAt,
                dateEpochDay = dateEpochDay
            )
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Date field (with picker) ---
        OutlinedTextField(
            value = dateText,
            enabled = enabled,
            onValueChange = { tf ->
                onDateTextChange(tf)
            },
            label = { Text(stringResource(R.string.time_label_date)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = showErrors && dateText.text.isNotBlank() && parsedDate == null,
            supportingText = {
                if (showErrors && dateText.text.isNotBlank() && parsedDate == null) {
                    Text(stringResource(R.string.time_error_invalid_date, "07/10/2025"))
                }
            },
            trailingIcon = {
                if (enabled) {
                    IconButton(
                        onClick = {
                            DatePickerDialog(
                                ctx,
                                { _, y, m, d ->
                                    // m is 0-based
                                    val picked = "%02d/%02d/%04d".format(d, m + 1, y)
                                    onDateTextChange(TextFieldValue(picked))
                                },
                                preYear, preMonthZeroBased, preDay
                            ).show()
                        }
                    ) {
                        Icon(
                            Icons.Filled.DateRange,
                            contentDescription = stringResource(R.string.time_pick_date)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // --- Start / End time (HH:mm) ---
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = startTimeText,
                enabled = enabled,
                onValueChange = { tf ->
                    onStartTimeTextChange(tf.copy(text = formatAsHhMm(tf.text)))
                },
                label = { Text(stringResource(R.string.time_label_start)) },
                isError = showErrors && startTimeText.text.isNotBlank() && parsedStart == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = endTimeText,
                enabled = enabled,
                onValueChange = { tf ->
                    onEndTimeTextChange(tf.copy(text = formatAsHhMm(tf.text)))
                },
                label = { Text(stringResource(R.string.time_label_end)) },
                isError = showErrors &&
                        endTimeText.text.isNotBlank() &&
                        (parsedEnd == null || (parsedStart != null && parsedEnd != null && !endAfterStart)),
                supportingText = {
                    if (showErrors && parsedStart != null && parsedEnd != null && !endAfterStart) {
                        Text(stringResource(R.string.time_error_end_before_start))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/* -------------------------- Helpers (shared) -------------------------- */

// Mask HH:mm while typing (00..23 : 00..59)
fun formatAsHhMm(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(4) // HHmm
    val sb = StringBuilder()
    for (i in digits.indices) {
        sb.append(digits[i])
        if (i == 1) sb.append(':')
    }
    return sb.toString().take(5)
}

// ICU parser for dd/MM/yyyy -> Calendar (for DatePicker preselect)
private fun parseDdMmYyyyOrNull(text: String): Calendar? {
    if (text.length != 10) return null
    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
    return try {
        val date = fmt.parse(text) ?: return null
        Calendar.getInstance().apply { time = date }
    } catch (_: Exception) {
        null
    }
}

// java.time LocalDate parser (strict)
private val DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT)

private fun parseDdMmYyyyToLocalDateOrNull(s: String): LocalDate? =
    runCatching { LocalDate.parse(s.trim(), DATE_FMT) }.getOrNull()

// java.time LocalTime parser (strict)
private val TIME_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("H:mm").withResolverStyle(ResolverStyle.STRICT)

private fun parseHhMmOrNull(s: String): LocalTime? =
    runCatching { LocalTime.parse(s.trim(), TIME_FMT) }.getOrNull()
