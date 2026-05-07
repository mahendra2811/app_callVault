package com.callNest.app.data.local.seed

import com.callNest.app.data.local.dao.TagDao
import com.callNest.app.data.local.entity.TagEntity

/**
 * Inserts the nine canonical system tags from spec §3.6 the first time the
 * Room database is created.
 *
 * The palette below is the locked 6-color set used everywhere a tag chip
 * renders; tags cycle through it in declaration order so the user gets a
 * pleasing variety without having to pick colors manually.
 */
object DefaultTagsSeeder {

    /** Locked 6-color palette used by every tag picker / chip in the app. */
    val Palette: List<String> = listOf(
        "#4F7CFF", // accent blue
        "#1FB5A8", // accent teal
        "#E0A82E", // amber
        "#E5536B", // rose
        "#8266E5", // violet
        "#34A853"  // green
    )

    /** Names + emoji for the system tags, in display order. */
    private data class Seed(val name: String, val emoji: String)

    private val Seeds: List<Seed> = listOf(
        Seed("Inquiry",      "📝"), // 📝
        Seed("Customer",     "🤝"), // 🤝
        Seed("Vendor",       "🏬"), // 🏬
        Seed("Personal",     "👤"), // 👤
        Seed("Spam",         "🚫"), // 🚫
        Seed("Follow-up",    "⏰"),       // ⏰
        Seed("Quoted",       "💰"), // 💰
        Seed("Closed-won",   "🏆"), // 🏆
        Seed("Closed-lost",  "❌")        // ❌
    )

    /**
     * Inserts the system tags if they are missing. Idempotent — if a tag
     * with the same name already exists it is left untouched, so re-running
     * the seeder after a crash never duplicates rows.
     */
    suspend fun seed(dao: TagDao) {
        Seeds.forEachIndexed { idx, s ->
            if (dao.findByName(s.name) == null) {
                dao.insert(
                    TagEntity(
                        name = s.name,
                        colorHex = Palette[idx % Palette.size],
                        emoji = s.emoji,
                        isSystem = true,
                        sortOrder = idx
                    )
                )
            }
        }
    }
}
