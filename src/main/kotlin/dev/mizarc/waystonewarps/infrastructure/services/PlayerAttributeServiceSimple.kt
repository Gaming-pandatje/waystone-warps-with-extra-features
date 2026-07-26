package dev.mizarc.waystonewarps.infrastructure.services

import dev.mizarc.waystonewarps.application.services.ConfigService
import dev.mizarc.waystonewarps.application.services.PlayerAttributeService
import net.luckperms.api.LuckPermsProvider
import org.bukkit.Bukkit
import java.util.*

class PlayerAttributeServiceSimple(private val configService: ConfigService): PlayerAttributeService {

    private fun getLuckPermsMeta(playerId: UUID, key: String): String? {
        return try {
            val lp = LuckPermsProvider.get()
            val user = lp.userManager.getUser(playerId) ?: return null
            user.cachedData.metaData.getMetaValue(key)
        } catch (e: Exception) {
            null
        }
    }

    override fun getWarpLimit(playerId: UUID): Int {
        PermissionWarpLimit.get(playerId)?.let { return it }
        getLuckPermsMeta(playerId, "waystonewarps.warp_limit")?.toIntOrNull()?.let { return it }
        return configService.getWarpLimit()
    }

    override fun getTeleportCost(playerId: UUID): Double {
        PermissionWarpCost.get(playerId)?.let { return it }
        getLuckPermsMeta(playerId, "waystonewarps.teleport_cost")?.toDoubleOrNull()?.let { return it }
        return configService.getTeleportCostAmount()
    }

    override fun getTeleportTimer(playerId: UUID): Int {
        PermissionWarpTimer.get(playerId)?.let { return it }
        getLuckPermsMeta(playerId, "waystonewarps.teleport_timer")?.toIntOrNull()?.let { return it }
        return configService.getTeleportTimer()
    }

    override fun getTeleportCooldown(playerId: UUID): Int {
        PermissionWarpCooldown.get(playerId)?.let { return it }
        getLuckPermsMeta(playerId, "waystonewarps.teleport_cooldown")?.toIntOrNull()?.let { return it }
        return configService.getTeleportCooldown()
    }
}
