package dev.mizarc.waystonewarps.infrastructure.persistence.migrations

import co.aikar.idb.Database

class Migration5_AddWarpNotifications : Migration {
    override val fromVersion: Int = 4
    override val toVersion: Int = 5

    override fun migrate(db: Database) {
        db.executeUpdate("""
            CREATE TABLE IF NOT EXISTS warp_notifications (
                warpId TEXT NOT NULL,
                playerId TEXT NOT NULL,
                enabled INTEGER NOT NULL DEFAULT 1,
                PRIMARY KEY (warpId, playerId)
            );
        """.trimIndent())

        // Defensive: if the table already existed without the 'enabled' column
        // (e.g. from a failed or partial earlier migration), add it now.
        val columns = db.getResults("PRAGMA table_info(warp_notifications);")
            .mapNotNull { it.getString("name") }
        if (!columns.contains("enabled")) {
            db.executeUpdate(
                "ALTER TABLE warp_notifications ADD COLUMN enabled INTEGER NOT NULL DEFAULT 1;"
            )
        }
    }
}
