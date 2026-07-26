package dev.mizarc.waystonewarps.domain.notifications

import java.util.UUID

data class WarpNotification(
    val warpId: UUID,
    val playerId: UUID,
    var enabled: Boolean = true
)
