package com.callvault.app.ui.components.neo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation

/**
 * Inset neumorphic search field.
 *
 * Reads as concave (pressed-in) so it visually invites text entry. Used at
 * the top of Calls, My Contacts, Inquiries and Search screens.
 */
@Composable
fun NeoSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search calls, names, numbers"
) {
    NeoSurface(
        modifier = modifier.fillMaxWidth(),
        elevation = NeoElevation.ConcaveSmall,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = NeoColors.OnBaseMuted,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                cursorBrush = SolidColor(NeoColors.AccentBlue),
                textStyle = LocalTextStyle.current.copy(color = NeoColors.OnBase),
                modifier = Modifier
                    .padding(start = 32.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterStart),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
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

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NeoSearchBarPreview() {
    CallVaultTheme {
        var q by remember { mutableStateOf("") }
        NeoSearchBar(
            query = q,
            onQueryChange = { q = it },
            modifier = Modifier.padding(24.dp)
        )
    }
}
