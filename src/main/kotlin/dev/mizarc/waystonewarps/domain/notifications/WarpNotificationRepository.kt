package dev.mizarc.waystonewarps.domain.notifications

import java.util.UUID

interface WarpNotificationRepository {
    fun isEnabled(warpId: UUID, playerId: UUID): Boolean
    fun getByWarp(warpId: UUID): List<WarpNotification>
    fun set(notification: WarpNotification)
    fun remove(warpId: UUID, playerId: UUID)
    fun removeByWarp(warpId: UUID)
}
