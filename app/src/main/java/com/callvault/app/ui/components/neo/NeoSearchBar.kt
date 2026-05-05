package com.callvault.app.ui.components.neo

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.R
import com.callvault.app.ui.theme.BorderSoft
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation

/**
 * Inset neumorphic search field with a 1.dp soft border and a clear-X
 * trailing icon when the query is non-empty.
 */
@Composable
fun NeoSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search calls, names, numbers"
) {
    val shape = RoundedCornerShape(16.dp)
    NeoSurface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .border(1.dp, NeoColors.BorderSoft, shape),
        elevation = NeoElevation.ConcaveSmall,
        shape = shape
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
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
                    .padding(start = 36.dp, end = 40.dp)
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
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cv_search_clear_cd),
                        tint = NeoColors.OnBaseMuted
                    )
                }
            }
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

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, name = "withQuery")
@Composable
private fun NeoSearchBarWithQueryPreview() {
    CallVaultTheme {
        var q by remember { mutableStateOf("ravi distributors") }
        NeoSearchBar(
            query = q,
            onQueryChange = { q = it },
            modifier = Modifier.padding(24.dp)
        )
    }
}
