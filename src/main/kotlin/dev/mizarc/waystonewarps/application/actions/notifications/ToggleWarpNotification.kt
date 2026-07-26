package dev.mizarc.waystonewarps.application.actions.notifications

import dev.mizarc.waystonewarps.domain.notifications.WarpNotification
import dev.mizarc.waystonewarps.domain.notifications.WarpNotificationRepository
import java.util.UUID

class ToggleWarpNotification(
    private val warpNotificationRepository: WarpNotificationRepository
) {
    /**
     * Toggles the notification state for [playerId] on [warpId].
     * @return the new state (true = enabled, false = disabled)
     */
    fun execute(warpId: UUID, playerId: UUID): Boolean {
        val current = warpNotificationRepository.isEnabled(warpId, playerId)
        val newState = !current
        warpNotificationRepository.set(WarpNotification(warpId, playerId, newState))
        return newState
    }
}
