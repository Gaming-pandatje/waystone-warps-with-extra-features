package dev.mizarc.waystonewarps.interaction.listeners

import dev.mizarc.waystonewarps.api.events.WarpDiscoverEvent
import dev.mizarc.waystonewarps.api.events.WarpTeleportEvent
import dev.mizarc.waystonewarps.api.events.WarpUseEvent
import dev.mizarc.waystonewarps.domain.notifications.WarpNotificationRepository
import dev.mizarc.waystonewarps.domain.coowner.CoOwnerRepository
import dev.mizarc.waystonewarps.domain.warps.WarpRepository
import dev.mizarc.waystonewarps.interaction.messaging.PrimaryColourPalette
import dev.mizarc.waystonewarps.interaction.messaging.AccentColourPalette
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.util.UUID

class WarpNotificationListener(
    private val warpNotificationRepository: WarpNotificationRepository,
    private val coOwnerRepository: CoOwnerRepository,
    private val warpRepository: WarpRepository
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onWarpUsed(event: WarpUseEvent) {
        val warp = event.warp
        val actorId = event.playerId
        // "Used" means someone opened the warp block UI (interaction)
        notifyInterestedPlayers(
            warpId = warp.id,
            ownerId = warp.playerId,
            actorId = actorId,
            message = buildMessage(actorId, warp.name, "used")
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onWarpTeleported(event: WarpTeleportEvent) {
        val warp = event.warp
        val actorId = event.playerId
        notifyInterestedPlayers(
            warpId = warp.id,
            ownerId = warp.playerId,
            actorId = actorId,
            message = buildMessage(actorId, warp.name, "teleported to")
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onWarpDiscovered(event: WarpDiscoverEvent) {
        // Fetch the warp from the warpId stored in the event
        val warp = warpRepository.getById(event.warpId) ?: return
        val actorId = event.playerId
        notifyInterestedPlayers(
            warpId = warp.id,
            ownerId = warp.playerId,
            actorId = actorId,
            message = buildMessage(actorId, warp.name, "discovered")
        )
    }

    // ---

    private fun notifyInterestedPlayers(
        warpId: UUID,
        ownerId: UUID,
        actorId: UUID,
        message: Component
    ) {
        // Build the set of players who should potentially receive this notification:
        // owner + all co-owners
        val candidates = mutableSetOf(ownerId)
        candidates.addAll(coOwnerRepository.getByWarp(warpId))

        // Don't notify the player who triggered the event
        candidates.remove(actorId)

        for (candidateId in candidates) {
            if (warpNotificationRepository.isEnabled(warpId, candidateId)) {
                Bukkit.getPlayer(candidateId)?.sendMessage(message)
            }
        }
    }

    private fun buildMessage(actorId: UUID, warpName: String, verb: String): Component {
        val actorName = Bukkit.getOfflinePlayer(actorId).name ?: actorId.toString()
        return Component.text()
            .append(Component.text("[Waystone] ").color(PrimaryColourPalette.PRIMARY.color))
            .append(Component.text(actorName).color(AccentColourPalette.INFO.color))
            .append(Component.text(" $verb ").color(PrimaryColourPalette.PRIMARY.color))
            .append(Component.text(warpName).color(AccentColourPalette.SUCCESS.color))
            .build()
    }
}
