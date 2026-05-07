package com.callNest.app.ui.util

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.callNest.app.ui.theme.NeoColors

/**
 * Minimal Markdown renderer for Notes.
 *
 * Supported syntax:
 * - `**bold**`
 * - `*italic*`
 * - bullet lines starting with `- `
 * - links of the form `[text](url)`
 *
 * Anything else renders as literal text. We deliberately avoid heavy markdown
 * libraries to keep notes lossless and predictable.
 */
@Composable
fun MarkdownText(
    source: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        source.lines().forEachIndexed { idx, raw ->
            val line = raw.trimEnd()
            if (line.startsWith("- ")) {
                Row(modifier = Modifier.padding(top = if (idx == 0) 0.dp else 2.dp)) {
                    Text(text = "•", color = NeoColors.OnBaseMuted)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = renderInline(line.removePrefix("- ")),
                        color = NeoColors.OnBase,
                        style = LocalTextStyle.current
                    )
                }
            } else {
                Text(
                    text = renderInline(line),
                    color = NeoColors.OnBase,
                    style = LocalTextStyle.current,
                    modifier = Modifier.padding(top = if (idx == 0) 0.dp else 2.dp)
                )
            }
        }
    }
}

/** Public for testing — converts the inline subset to an AnnotatedString. */
fun renderInline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        // Bold: **...**
        if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
            val end = text.indexOf("**", startIndex = i + 2)
            if (end > 0) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(text.substring(i + 2, end))
                pop()
                i = end + 2
                continue
            }
        }
        // Italic: *...*
        if (text[i] == '*') {
            val end = text.indexOf('*', startIndex = i + 1)
            if (end > 0) {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(text.substring(i + 1, end))
                pop()
                i = end + 1
                continue
            }
        }
        // Link: [text](url)
        if (text[i] == '[') {
            val close = text.indexOf(']', startIndex = i + 1)
            if (close > 0 && close + 1 < text.length && text[close + 1] == '(') {
                val urlEnd = text.indexOf(')', startIndex = close + 2)
                if (urlEnd > 0) {
                    val label = text.substring(i + 1, close)
                    pushStyle(
                        SpanStyle(
                            color = NeoColors.AccentBlue,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                    append(label)
                    pop()
                    i = urlEnd + 1
                    continue
                }
            }
        }
        append(text[i])
        i++
    }
}
