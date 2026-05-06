package com.callvault.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema migration registry.
 *
 * - v1 → v2 (Sprint 6): introduces `rule_score_boosts` for [com.callvault.app.data.local.entity.RuleScoreBoostEntity].
 *
 * Append `Migration(from, to)` entries here in numerical order on every bump.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `rule_score_boosts` (
                `callSystemId` INTEGER NOT NULL,
                `ruleId` INTEGER NOT NULL,
                `delta` INTEGER NOT NULL,
                `appliedAt` INTEGER NOT NULL,
                PRIMARY KEY(`callSystemId`, `ruleId`),
                FOREIGN KEY(`callSystemId`) REFERENCES `calls`(`systemId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_rule_score_boosts_ruleId` ON `rule_score_boosts` (`ruleId`)"
        )
    }
}

/**
 * v2 → v3: adds `whatsappTemplate` to `tags` so each tag can carry an
 * optional pre-filled WhatsApp Business message.
 */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tags` ADD COLUMN `whatsappTemplate` TEXT DEFAULT NULL")
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
