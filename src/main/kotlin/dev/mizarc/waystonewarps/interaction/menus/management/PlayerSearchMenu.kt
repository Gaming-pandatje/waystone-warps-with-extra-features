package dev.mizarc.waystonewarps.interaction.menus.management

import dev.mizarc.waystonewarps.interaction.localization.LocalizationProvider
import dev.mizarc.waystonewarps.interaction.menus.Menu
import dev.mizarc.waystonewarps.interaction.menus.MenuNavigator
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PlayerSearchMenu(
    private val player: Player,
    private val menuNavigator: MenuNavigator
): Menu, KoinComponent {
    private val localizationProvider: LocalizationProvider by inject()
    private val plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("WaystoneWarps")!!

    private fun isBedrockPlayer(): Boolean {
        return try {
            org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(player.uniqueId)
        } catch (e: Exception) { false }
    }

    override fun open() {
        if (isBedrockPlayer()) openBedrockForm() else openChatInput()
    }

    private fun openBedrockForm() {
        try {
            val floodgateApi = org.geysermc.floodgate.api.FloodgateApi.getInstance()
            val form = org.geysermc.cumulus.form.CustomForm.builder()
                .title("Search Player")
                .input("Enter player name here", "")
                .validResultHandler { response ->
                    val input = response.asInput(0)?.trim() ?: return@validResultHandler
                    plugin.server.scheduler.runTask(plugin, Runnable { menuNavigator.goBackWithData(input) })
                }
                .build()
            floodgateApi.sendForm(player.uniqueId, form)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to open Bedrock form for ${player.name}: ${e.message}")
        }
    }

    private fun openChatInput() {
        player.closeInventory()
        player.sendMessage("§6Type a player name in chat (or type §ccancel§6 to abort):")
        val listener = object : Listener {
            @EventHandler
            fun onChat(event: AsyncPlayerChatEvent) {
                if (event.player.uniqueId != player.uniqueId) return
                event.isCancelled = true
                HandlerList.unregisterAll(this)
                val input = event.message.trim()
                if (input.equals("cancel", ignoreCase = true)) {
                    plugin.server.scheduler.runTask(plugin, Runnable { menuNavigator.goBack() })
                    return
                }
                plugin.server.scheduler.runTask(plugin, Runnable { menuNavigator.goBackWithData(input) })
            }
        }
        plugin.server.pluginManager.registerEvents(listener, plugin)
    }
}
