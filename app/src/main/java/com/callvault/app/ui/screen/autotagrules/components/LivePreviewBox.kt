package com.callvault.app.ui.screen.autotagrules.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.callvault.app.R
import com.callvault.app.ui.components.neo.NeoSurface
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation

/**
 * Banner that shows the live "matches X / latest 200 calls" preview while the
 * user edits a rule. The actual evaluation is performed inside the
 * [com.callvault.app.ui.screen.autotagrules.RuleEditorViewModel] with a 400 ms
 * debounce — this composable is purely presentational.
 */
@Composable
fun LivePreviewBox(matchCount: Int, modifier: Modifier = Modifier) {
    NeoSurface(
        modifier = modifier.fillMaxWidth(),
        elevation = NeoElevation.Flat,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.rule_editor_preview_match_count, matchCount),
                color = NeoColors.OnBaseMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
