package com.example.mycontrolapp.ui.componentes.custom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.example.mycontrolapp.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CandidatesInput(
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int = 0,
    max: Int = 10,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "candidates",
    initialUseSlider: Boolean = true,
) {
    require(min <= max) { "min must be <= max" }

    var useSlider by rememberSaveable { mutableStateOf(initialUseSlider) }
    var sliderValue by rememberSaveable { mutableFloatStateOf(value.coerceIn(min, max).toFloat()) }
    var text by rememberSaveable { mutableStateOf(value.coerceIn(min, max).toString()) }

    // Keep internal states in sync if parent value changes externally
    LaunchedEffect(value) {
        val clamped = value.coerceIn(min, max)
        if (clamped != sliderValue.toInt()) sliderValue = clamped.toFloat()
        val clampedStr = clamped.toString()
        if (text != clampedStr) text = clampedStr
    }

    Column( verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(
                    R.string.candidates_input_mode,
                    if (useSlider) stringResource(R.string.mode_slider) else stringResource(R.string.mode_text)
                )
            )
            Switch(
                checked = useSlider,
                onCheckedChange = { useSlider = it },
                modifier = Modifier.testTag("${testTagPrefix}_mode_switch")
            )
        }

        if (useSlider) {
            Text(stringResource(R.string.select_number_of_candidates))
            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    val v = it.roundToInt().coerceIn(min, max)
                    val s = v.toString()
                    if (text != s) text = s
                    onValueChange(v)
                },
                valueRange = min.toFloat()..max.toFloat(),
                steps = (max - min).coerceAtLeast(1) - 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_slider")
            )
            Text(stringResource(R.string.selected_n, sliderValue.roundToInt()))
        } else {
            OutlinedTextField(
                value = text,
                onValueChange = { typed ->
                    // allow typing and deleting (including empty)
                    val digitsOnly = typed.filter { it.isDigit() }
                    text = digitsOnly

                    val parsed = digitsOnly.toIntOrNull()
                    if (parsed != null) {
                        val clamped = parsed.coerceIn(min, max)
                        if (sliderValue.toInt() != clamped) sliderValue = clamped.toFloat()
                        onValueChange(clamped)
                    }
                    // if empty or invalid, don't push changes up yet; wait until valid
                },
                label = { Text(stringResource(R.string.number_of_candidates_range, min, max)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                isError = text.isNotEmpty() && (text.toIntOrNull() !in min..max),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_text")
            )
        }
    }
}