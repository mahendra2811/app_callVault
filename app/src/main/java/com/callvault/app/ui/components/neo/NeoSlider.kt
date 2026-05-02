package com.callvault.app.ui.components.neo

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors

/**
 * Continuous neumorphic slider.
 *
 * Wraps Material 3's [Slider] with the locked color palette so the active
 * track reads as accent blue and the inactive track stays muted against the
 * neumorphic base.
 */
@Composable
fun NeoSlider(
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    steps: Int = 0
) {
    Slider(
        value = value,
        onValueChange = onChange,
        modifier = modifier.height(48.dp),
        valueRange = range,
        steps = steps,
        enabled = enabled,
        colors = SliderDefaults.colors(
            thumbColor = NeoColors.Light,
            activeTrackColor = NeoColors.AccentBlue,
            inactiveTrackColor = NeoColors.Inset,
            disabledThumbColor = NeoColors.OnBaseSubtle,
            disabledActiveTrackColor = NeoColors.OnBaseSubtle,
            disabledInactiveTrackColor = NeoColors.Inset
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NeoSliderPreview() {
    CallVaultTheme {
        var v by remember { mutableFloatStateOf(0.4f) }
        NeoSlider(
            value = v,
            onChange = { v = it },
            modifier = Modifier.padding(24.dp).fillMaxWidth()
        )
    }
}
