package com.callvault.app.ui.screen.calldetail.sections

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.components.neo.NeoIconButton

/**
 * Primary action row: Call · Message · WhatsApp · Save · Block.
 *
 * All buttons fire real Android intents; the host activity handles the
 * `ActivityNotFoundException` if a target app isn't installed (rare but
 * possible for WhatsApp).
 */
@Composable
fun ActionBar(
    normalizedNumber: String,
    displayName: String? = null,
    onSaveToContacts: () -> Unit,
    onBlock: () -> Unit,
    onManageTemplates: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    var quickReplyOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NeoIconButton(
            icon = Icons.Filled.Phone,
            contentDescription = "Call",
            onClick = {
                val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$normalizedNumber"))
                ctx.startActivity(i)
            }
        )
        NeoIconButton(
            icon = Icons.AutoMirrored.Filled.Message,
            contentDescription = "Message",
            onClick = {
                val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$normalizedNumber"))
                ctx.startActivity(i)
            }
        )
        NeoIconButton(
            icon = Icons.AutoMirrored.Filled.Chat,
            contentDescription = "WhatsApp",
            onClick = { quickReplyOpen = true },
        )
        NeoIconButton(
            icon = Icons.Filled.PersonAdd,
            contentDescription = "Save to contacts",
            onClick = {
                val i = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                    type = ContactsContract.RawContacts.CONTENT_TYPE
                    putExtra(ContactsContract.Intents.Insert.PHONE, normalizedNumber)
                }
                runCatching { ctx.startActivity(i) }
                onSaveToContacts()
            }
        )
        NeoIconButton(
            icon = Icons.Filled.Block,
            contentDescription = "Block",
            onClick = onBlock
        )
    }

    if (quickReplyOpen) {
        WhatsAppQuickReplySheet(
            normalizedNumber = normalizedNumber,
            displayName = displayName,
            onDismiss = { quickReplyOpen = false },
            onManageTemplates = onManageTemplates,
        )
    }
}
