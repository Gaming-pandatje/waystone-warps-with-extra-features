package dev.mizarc.waystonewarps.interaction.menus.management

import dev.mizarc.waystonewarps.application.actions.management.UpdateWarpName
import dev.mizarc.waystonewarps.application.actions.coowner.GetCoOwners
import dev.mizarc.waystonewarps.application.results.UpdateWarpNameResult
import dev.mizarc.waystonewarps.domain.warps.Warp
import dev.mizarc.waystonewarps.interaction.localization.LocalizationKeys
import dev.mizarc.waystonewarps.interaction.localization.LocalizationProvider
import dev.mizarc.waystonewarps.interaction.menus.Menu
import dev.mizarc.waystonewarps.interaction.menus.MenuNavigator
import dev.mizarc.waystonewarps.interaction.messaging.PrimaryColourPalette
import dev.mizarc.waystonewarps.interaction.utils.PermissionHelper
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WarpRenamingMenu(
    private val player: Player,
    private val menuNavigator: MenuNavigator,
    private val warp: Warp,
    private val localizationProvider: LocalizationProvider
) : Menu, KoinComponent {
    private val updateWarpName: UpdateWarpName by inject()
    private val getCoOwners: GetCoOwners by inject()
    private val plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("WaystoneWarps")!!

    private fun isBedrockPlayer(): Boolean {
        return try {
            org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(player.uniqueId)
        } catch (e: Exception) { false }
    }

    override fun open() {
        val canRename = PermissionHelper.canRename(player, warp.playerId, getCoOwners.execute(warp.id))
        if (!canRename) {
            player.sendMessage("§c${localizationProvider.get(player.uniqueId, LocalizationKeys.MENU_WARP_MANAGEMENT_COMMON_NO_PERMISSION)}")
            menuNavigator.goBack()
            return
        }
        if (isBedrockPlayer()) openBedrockForm() else openChatInput()
    }

    private fun openBedrockForm() {
        try {
            val floodgateApi = org.geysermc.floodgate.api.FloodgateApi.getInstance()
            val form = org.geysermc.cumulus.form.CustomForm.builder()
                .title("Rename Waystone")
                .input("Enter new name here", "")
                .validResultHandler { response ->
                    val input = response.asInput(0)?.trim() ?: return@validResultHandler
                    plugin.server.scheduler.runTask(plugin, Runnable { submitName(input) })
                }
                .build()
            floodgateApi.sendForm(player.uniqueId, form)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to open Bedrock form for ${player.name}: ${e.message}")
        }
    }

    private fun openChatInput() {
        player.closeInventory()
        player.sendMessage("§6Type a new name for §e${warp.name}§6 in chat (or type §ccancel§6 to abort):")
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
                plugin.server.scheduler.runTask(plugin, Runnable { submitName(input) })
            }
        }
        plugin.server.pluginManager.registerEvents(listener, plugin)
    }

    private fun submitName(inputName: String) {
        if (inputName == warp.name) { menuNavigator.goBack(); return }
        val result = updateWarpName.execute(
            warpId = warp.id, editorPlayerId = player.uniqueId, name = inputName,
            bypassOwnership = player.hasPermission("waystonewarps.bypass.rename")
                    || getCoOwners.execute(warp.id).contains(player.uniqueId)
        )
        when (result) {
            UpdateWarpNameResult.SUCCESS -> menuNavigator.goBack()
            UpdateWarpNameResult.WARP_NOT_FOUND -> {
                player.sendMessage("§c${localizationProvider.get(player.uniqueId, LocalizationKeys.CONDITION_NAMING_NOT_FOUND)}")
                menuNavigator.goBack()
            }
            UpdateWarpNameResult.NAME_ALREADY_TAKEN -> {
                player.sendMessage("§c${localizationProvider.get(player.uniqueId, LocalizationKeys.CONDITION_NAMING_EXISTING, inputName)}")
                if (!isBedrockPlayer()) openChatInput() else openBedrockForm()
            }
            UpdateWarpNameResult.NAME_BLANK -> {
                player.sendMessage("§cName cannot be blank.")
                if (!isBedrockPlayer()) openChatInput() else openBedrockForm()
            }
            UpdateWarpNameResult.NOT_AUTHORIZED -> {
                player.sendMessage("§c${localizationProvider.get(player.uniqueId, LocalizationKeys.CONDITION_NAMING_NO_PERMISSION)}")
                menuNavigator.goBack()
            }
        }
    }
}
