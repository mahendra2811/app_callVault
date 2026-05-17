package com.callNest.app.ui.screen.calldetail.sections

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.callNest.app.R
import com.callNest.app.ui.components.neo.NeoIconButton
import com.callNest.app.ui.theme.SageColors

/**
 * Primary action row: Call · Message · WhatsApp · WhatsApp Business · Save.
 *
 * Block was removed for v1.0.x — Android's own dialer-level "Block this
 * number" handles the use case and double-blocking from inside the app
 * confused testers. Re-add here if there's user demand.
 *
 * Each WhatsApp button opens its own QuickReply sheet that routes through
 * the matching package (`com.whatsapp` / `com.whatsapp.w4b`).
 */
@Composable
fun ActionBar(
    normalizedNumber: String,
    displayName: String? = null,
    onSaveToContacts: () -> Unit,
    onManageTemplates: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    var quickReplyOpen by remember { mutableStateOf(false) }
    var quickReplyBizOpen by remember { mutableStateOf(false) }
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
        // Two WhatsApp surfaces side by side with W / WB labels so testers
        // can tell them apart at a glance. Tap routes to the right package
        // (com.whatsapp / com.whatsapp.w4b) — see WhatsAppQuickReplySheet.
        LabeledIcon(
            painterId = R.drawable.ic_whatsapp,
            contentDescription = "WhatsApp",
            label = "W",
            onClick = { quickReplyOpen = true }
        )
        LabeledIcon(
            painterId = R.drawable.ic_whatsapp_business,
            contentDescription = "WhatsApp Business",
            label = "WB",
            onClick = { quickReplyBizOpen = true }
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
    }

    if (quickReplyOpen) {
        WhatsAppQuickReplySheet(
            normalizedNumber = normalizedNumber,
            displayName = displayName,
            onDismiss = { quickReplyOpen = false },
            onManageTemplates = onManageTemplates,
            business = false,
        )
    }
    if (quickReplyBizOpen) {
        WhatsAppQuickReplySheet(
            normalizedNumber = normalizedNumber,
            displayName = displayName,
            onDismiss = { quickReplyBizOpen = false },
            onManageTemplates = onManageTemplates,
            business = true,
        )
    }
}

/** NeoIconButton with a tiny one- or two-letter label underneath. */
@Composable
private fun LabeledIcon(
    painterId: Int,
    contentDescription: String,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        NeoIconButton(
            painter = painterResource(painterId),
            contentDescription = contentDescription,
            onClick = onClick,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = SageColors.TextSecondary
        )
    }
}
