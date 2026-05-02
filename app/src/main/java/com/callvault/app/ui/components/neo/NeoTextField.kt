package com.callvault.app.ui.components.neo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation

/**
 * Inset neumorphic text field with an optional leading icon and a label
 * rendered above the field.
 *
 * Material 3's `TextField` ships with too much chrome for the neumorphic look,
 * so this component composes [BasicTextField] inside a concave [NeoSurface].
 */
@Composable
fun NeoTextField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    placeholder: String = "",
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = NeoColors.OnBaseMuted,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        NeoSurface(
            modifier = Modifier.fillMaxWidth(),
            elevation = NeoElevation.ConcaveSmall,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = NeoColors.OnBaseMuted
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    BasicTextField(
                        value = value,
                        onValueChange = onChange,
                        enabled = enabled,
                        singleLine = true,
                        cursorBrush = SolidColor(NeoColors.AccentBlue),
                        textStyle = LocalTextStyle.current.copy(color = NeoColors.OnBase),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (value.isEmpty() && placeholder.isNotEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = NeoColors.OnBaseSubtle,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            inner()
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NeoTextFieldPreview() {
    CallVaultTheme {
        var v by remember { mutableStateOf("") }
        NeoTextField(
            value = v,
            onChange = { v = it },
            label = "Display name",
            placeholder = "Enter your name",
            modifier = Modifier.padding(24.dp)
        )
    }
}
